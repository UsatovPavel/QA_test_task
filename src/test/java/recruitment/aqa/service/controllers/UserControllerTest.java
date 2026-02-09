package recruitment.aqa.service.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import recruitment.aqa.service.Result;
import recruitment.aqa.service.configs.SecurityConfiguration;
import recruitment.aqa.service.services.ActionService;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import({ExceptionTranslator.class, ResponseContextAdvice.class, SecurityConfiguration.class})
@TestPropertySource(properties = "secret=qazWSXedc")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActionService actionService;

    // --- SECURITY TESTS ---
    
    @Test
    @DisplayName("Security Check: Missing X-Api-Key should return 401 Unauthorized")
    void endpoint_MissingApiKey_ShouldReturnUnauthorized() throws Exception {
        
        mockMvc.perform(post("/endpoint")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", "1234567890ABCDEF1234567890ABCDEF")
                .param("action", "LOGIN"))
                .andExpect(status().isUnauthorized()); // Validating that security works!
    }
    @Test
    @DisplayName("Security: Stateless check - subsequent request without Key fails")
    void endpoint_SubsequentRequestWithoutKey_ReturnsUnauthorized() throws Exception {
        // 1. Successful request
        String token = "1234567890ABCDEF1234567890ABCDEF";
        mockMvc.perform(post("/endpoint")
                .header("X-API-Key", "qazWSXedc") // With Key
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", token)
                .param("action", "LOGIN"))
                .andExpect(status().isOk());

        // 2. Request without Key (should fail, proving no session leakage)
        mockMvc.perform(post("/endpoint")
                // No Key
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", token)
                .param("action", "ACTION"))
                .andExpect(status().isUnauthorized());
    }

    // --- FUNCTIONAL TESTS ---

    @Test
    @DisplayName("Valid LOGIN Request (with correct API Key) should return 200 OK")
    void endpoint_ValidLogin_ReturnsOk() throws Exception {
        String token = "1234567890ABCDEF1234567890ABCDEF";
        doNothing().when(actionService).auth(token);

        mockMvc.perform(post("/endpoint")
                .header("X-API-Key", "qazWSXedc") // Providing the correct key
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", token)
                .param("action", "LOGIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(Result.OK.name()));
    }

    @Test
    @DisplayName("Valid ACTION Request (with correct API Key) should return 200 OK")
    void endpoint_ValidAction_ReturnsOk() throws Exception {
        String token = "1234567890ABCDEF1234567890ABCDEF";
        doNothing().when(actionService).action(token);

        mockMvc.perform(post("/endpoint")
                .header("X-API-Key", "qazWSXedc") // Providing the key
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", token)
                .param("action", "ACTION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(Result.OK.name()));
    }

    @Test
    @DisplayName("Invalid Action Enum should return 400 Bad Request")
    void endpoint_InvalidAction_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/endpoint")
                .header("X-API-Key", "qazWSXedc")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", "1234567890ABCDEF1234567890ABCDEF")
                .param("action", "UNKNOWN_ACTION"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.result").value("ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("invalid action")));
    }

    // --- ERROR HANDLING TESTS (ExceptionTranslator) ---

    @Test
    @DisplayName("Token Already Exists should return 409 Conflict")
    void endpoint_TokenAlreadyExists_ReturnsConflict() throws Exception {
        String token = "1234567890ABCDEF1234567890ABCDEF";
        // Construct exception with the token, assuming the exception formats the message using it
        doThrow(new recruitment.aqa.service.exceptions.TokenAlreadyExists(token))
                .when(actionService).auth(token);

        mockMvc.perform(post("/endpoint")
                .header("X-API-Key", "qazWSXedc")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", token)
                .param("action", "LOGIN"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.result").value("ERROR"))
                // Expect the error message to contain the token (exact format depends on exception implementation)
                .andExpect(jsonPath("$.message").value(containsString(token)));
    }

    @Test
    @DisplayName("Token Not Found should return 403 Forbidden")
    void endpoint_TokenNotFound_ReturnsForbidden() throws Exception {
        String token = "1234567890ABCDEF1234567890ABCDEF";
        doThrow(new recruitment.aqa.service.exceptions.TokenNotFound(token))
                .when(actionService).action(token);

        mockMvc.perform(post("/endpoint")
                .header("X-API-Key", "qazWSXedc")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", token)
                .param("action", "ACTION"))
                .andExpect(status().isForbidden()) // 403 from ExceptionTranslator
                .andExpect(jsonPath("$.result").value("ERROR"))
                .andExpect(jsonPath("$.message").value(containsString(token)));
    }

    @Test
    @DisplayName("Validation Error (empty token) should return 400 Bad Request")
    void endpoint_ValidationError_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/endpoint")
                .header("X-API-Key", "qazWSXedc")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", "") // Empty token -> Validation Error
                .param("action", "LOGIN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.result").value("ERROR"));
    }

    @Test
    @DisplayName("Validation Error: Missing 'token' parameter should return 400 Bad Request")
    void endpoint_MissingTokenParam_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/endpoint")
                .header("X-API-Key", "qazWSXedc")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                // Missing token parameter
                .param("action", "LOGIN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.result").value("ERROR"));
    }

    @Test
    @DisplayName("Internal Server Error should return 500")
    void endpoint_InternalError_Returns500() throws Exception {
        String token = "1234567890ABCDEF1234567890ABCDEF";
        doThrow(new RuntimeException("Something went wrong"))
                .when(actionService).logout(token);

        mockMvc.perform(post("/endpoint")
                .header("X-API-Key", "qazWSXedc")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", token)
                .param("action", "LOGOUT"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.result").value("ERROR"))
                .andExpect(jsonPath("$.message").value("Internal Server Error"));
    }

    @Test
    @DisplayName("Invalid API Key should return 401 Unauthorized")
    void endpoint_InvalidApiKey_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/endpoint")
                .header("X-API-Key", "INVALID_KEY") 
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", "1234567890ABCDEF1234567890ABCDEF")
                .param("action", "LOGIN"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Valid LOGOUT Request should return 200 OK")
    void endpoint_ValidLogout_ReturnsOk() throws Exception {
        String token = "1234567890ABCDEF1234567890ABCDEF";
        doNothing().when(actionService).logout(token);

        mockMvc.perform(post("/endpoint")
                .header("X-API-Key", "qazWSXedc")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", token)
                .param("action", "LOGOUT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("OK"));
    }

    @Test
    @DisplayName("External Service Error (HttpStatusCodeException) should return 500 Internal Server Error")
    void endpoint_ExternalServiceError_ReturnsInternalServerError() throws Exception {
        String token = "1234567890ABCDEF1234567890ABCDEF"; // Valid HEX token
        // Mocking HttpClientErrorException (subclass of HttpStatusCodeException)
        doThrow(new org.springframework.web.client.HttpClientErrorException(org.springframework.http.HttpStatus.BAD_REQUEST))
                .when(actionService).action(token);

        mockMvc.perform(post("/endpoint")
                .header("X-API-Key", "qazWSXedc")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", token)
                .param("action", "ACTION"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.result").value("ERROR"))
                .andExpect(jsonPath("$.message").value("Internal Server Error"));
    }

    @Test
    @DisplayName("Empty API Key should return 401 Unauthorized")
    void endpoint_EmptyApiKey_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/endpoint")
                .header("X-API-Key", "") // Empty Key -> isBlank branch
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", "1234567890ABCDEF1234567890ABCDEF")
                .param("action", "LOGIN"))
                .andExpect(status().isUnauthorized());
    }


}
