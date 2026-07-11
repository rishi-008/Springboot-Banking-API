package com.rishi.digitalbankingapi.account;

import com.rishi.digitalbankingapi.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AccountControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createGetAndDepositAccount() throws Exception {
        String token = registerAndLogin();

        String createResponse = mockMvc.perform(post("/api/accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ownerName":"Alice","accountType":"CHECKING","openingBalance":100.00}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.balance").value(100.00))
                .andReturn().getResponse().getContentAsString();

        String accountNumber = extractField(createResponse, "accountNumber");

        mockMvc.perform(get("/api/accounts/" + accountNumber).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerName").value("Alice"));

        mockMvc.perform(post("/api/accounts/" + accountNumber + "/deposit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":50.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(150.00));
    }

    @Test
    void createAccountWithBlankOwnerNameReturnsValidationError() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(post("/api/accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ownerName":"","accountType":"SAVINGS"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("ownerName"));
    }

    @Test
    void getUnknownAccountReturnsNotFound() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(get("/api/accounts/0000000000").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
    }

    @Test
    void withdrawBeyondBalanceReturnsConflict() throws Exception {
        String token = registerAndLogin();
        String accountNumber = createAccount(token, "50.00");

        mockMvc.perform(post("/api/accounts/" + accountNumber + "/withdraw")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":500.00}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_FUNDS"));
    }

    @Test
    void listAllAccountsIsAdminOnly() throws Exception {
        String userToken = registerAndLogin();
        createAccount(userToken, "10.00");

        mockMvc.perform(get("/api/accounts").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        String adminToken = login("admin", "admin12345");
        mockMvc.perform(get("/api/accounts").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void closeAccountIsAdminOnlyAndBlocksFurtherTransactions() throws Exception {
        String userToken = registerAndLogin();
        String accountNumber = createAccount(userToken, "10.00");

        mockMvc.perform(delete("/api/accounts/" + accountNumber).header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        String adminToken = login("admin", "admin12345");
        mockMvc.perform(delete("/api/accounts/" + accountNumber).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/accounts/" + accountNumber + "/deposit")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":10.00}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_ACTIVE"));
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
        return extractField(response, "accountNumber");
    }

    private String registerAndLogin() throws Exception {
        String username = "user-" + UUID.randomUUID();
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"%s\",\"password\":\"password123\"}".formatted(username)));
        return login(username, "password123");
    }

    private String login(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password)))
                .andReturn().getResponse().getContentAsString();
        return extractField(response, "token");
    }

    private String extractField(String json, String field) {
        return json.split("\"" + field + "\":\"")[1].split("\"")[0];
    }
}
