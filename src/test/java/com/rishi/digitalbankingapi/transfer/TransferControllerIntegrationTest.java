package com.rishi.digitalbankingapi.transfer;

import com.rishi.digitalbankingapi.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransferControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void transferMovesFundsBetweenAccounts() throws Exception {
        String token = registerAndLogin();
        String accountA = createAccount(token, "500.00");
        String accountB = createAccount(token, "0.00");

        mockMvc.perform(post("/api/transfers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson(accountA, accountB, "200.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromAccount.balance").value(300.00))
                .andExpect(jsonPath("$.toAccount.balance").value(200.00));
    }

    @Test
    void transferBeyondBalanceReturnsConflict() throws Exception {
        String token = registerAndLogin();
        String accountA = createAccount(token, "10.00");
        String accountB = createAccount(token, "0.00");

        mockMvc.perform(post("/api/transfers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson(accountA, accountB, "5000.00")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_FUNDS"));
    }

    @Test
    void transferToSameAccountReturnsBadRequest() throws Exception {
        String token = registerAndLogin();
        String accountA = createAccount(token, "100.00");

        mockMvc.perform(post("/api/transfers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson(accountA, accountA, "10.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TRANSFER"));
    }

    /**
     * Proves the pessimistic row locking in TransferServiceImpl actually
     * prevents lost updates: 10 threads concurrently transfer 10.00 each
     * from the same source account. Without locking, concurrent
     * read-modify-write cycles on the same row would lose some of these
     * debits/credits, and the final balance would land somewhere higher
     * than expected.
     */
    @Test
    void concurrentTransfersFromSameAccountDoNotLoseUpdates() throws Exception {
        String token = registerAndLogin();
        String accountA = createAccount(token, "1000.00");
        String accountB = createAccount(token, "0.00");
        int threadCount = 10;

        List<Callable<Integer>> tasks = IntStream.range(0, threadCount)
                .<Callable<Integer>>mapToObj(i -> () -> mockMvc.perform(post("/api/transfers")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(transferJson(accountA, accountB, "10.00")))
                        .andReturn().getResponse().getStatus())
                .toList();

        List<Integer> results = runConcurrently(tasks);

        assertThat(results).allMatch(status -> status == 200);
        assertThat(getBalance(token, accountA)).isEqualByComparingTo("900.00");
        assertThat(getBalance(token, accountB)).isEqualByComparingTo("100.00");
    }

    /**
     * Proves the ascending-account-number lock ordering in
     * TransferServiceImpl avoids deadlocks: fires concurrent transfers in
     * BOTH directions between the same two accounts. If lock acquisition
     * instead followed transfer direction, opposite-direction transfers
     * racing each other would deadlock and Postgres would abort one side
     * with a "deadlock detected" error instead of a clean 200.
     */
    @Test
    void concurrentOppositeDirectionTransfersDoNotDeadlock() throws Exception {
        String token = registerAndLogin();
        String accountA = createAccount(token, "1000.00");
        String accountB = createAccount(token, "1000.00");
        int threadCount = 10;

        List<Callable<Integer>> tasks = IntStream.range(0, threadCount)
                .<Callable<Integer>>mapToObj(i -> {
                    boolean aToB = i % 2 == 0;
                    String from = aToB ? accountA : accountB;
                    String to = aToB ? accountB : accountA;
                    return () -> mockMvc.perform(post("/api/transfers")
                                    .header("Authorization", "Bearer " + token)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(transferJson(from, to, "5.00")))
                            .andReturn().getResponse().getStatus();
                })
                .toList();

        List<Integer> results = runConcurrently(tasks);

        assertThat(results).allMatch(status -> status == 200);
        BigDecimal total = getBalance(token, accountA).add(getBalance(token, accountB));
        assertThat(total).isEqualByComparingTo("2000.00");
    }

    private List<Integer> runConcurrently(List<Callable<Integer>> tasks) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
        try {
            CountDownLatch readyLatch = new CountDownLatch(tasks.size());
            CountDownLatch startLatch = new CountDownLatch(1);

            List<Future<Integer>> futures = tasks.stream()
                    .map(task -> executor.submit(() -> {
                        readyLatch.countDown();
                        startLatch.await();
                        return task.call();
                    }))
                    .toList();

            readyLatch.await(5, TimeUnit.SECONDS);
            startLatch.countDown();

            List<Integer> results = new ArrayList<>();
            for (Future<Integer> future : futures) {
                results.add(future.get(30, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            executor.shutdown();
        }
    }

    private BigDecimal getBalance(String token, String accountNumber) throws Exception {
        String response = mockMvc.perform(get("/api/accounts/" + accountNumber)
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        return new BigDecimal(response.split("\"balance\":")[1].split("[,}]")[0]);
    }

    private String createAccount(String token, String openingBalance) throws Exception {
        String response = mockMvc.perform(post("/api/accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ownerName":"Test Owner","accountType":"CHECKING","openingBalance":%s}
                                """.formatted(openingBalance)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return response.split("\"accountNumber\":\"")[1].split("\"")[0];
    }

    private String registerAndLogin() throws Exception {
        String username = "user-" + UUID.randomUUID();
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"%s\",\"password\":\"password123\"}".formatted(username)));

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"password123\"}".formatted(username)))
                .andReturn().getResponse().getContentAsString();
        return response.split("\"token\":\"")[1].split("\"")[0];
    }

    private String transferJson(String from, String to, String amount) {
        return """
                {"fromAccountNumber":"%s","toAccountNumber":"%s","amount":%s}
                """.formatted(from, to, amount);
    }
}
