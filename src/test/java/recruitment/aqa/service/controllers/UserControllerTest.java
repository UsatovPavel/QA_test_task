package recruitment.aqa.service.controllers;

import io.qameta.allure.Allure;
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
    @DisplayName("Безопасность: Запрос без X-API-Key должен возвращать 401 Unauthorized")
    void endpoint_MissingApiKey_ShouldReturnUnauthorized() throws Exception {
        Allure.step("Выполняем POST запрос без заголовка X-API-Key");
        mockMvc.perform(post("/endpoint")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", "1234567890ABCDEF1234567890ABCDEF")
                .param("action", "LOGIN"))
                .andExpect(status().isUnauthorized()); // Validating that security works!
    }
    @Test
    @DisplayName("Безопасность: Проверка Stateless - повторный запрос без ключа отклоняется")
    void endpoint_SubsequentRequestWithoutKey_ReturnsUnauthorized() throws Exception {
        String token = "1234567890ABCDEF1234567890ABCDEF";
        
        Allure.step("Шаг 1: Выполняем успешный запрос с валидным ключом", () -> {
            mockMvc.perform(post("/endpoint")
                    .header("X-API-Key", "qazWSXedc")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("token", token)
                    .param("action", "LOGIN"))
                    .andExpect(status().isOk());
        });

        Allure.step("Шаг 2: Выполняем запрос без ключа (должен упасть, доказывая отсутствие сессий)", () -> {
            mockMvc.perform(post("/endpoint")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("token", token)
                    .param("action", "ACTION"))
                    .andExpect(status().isUnauthorized());
        });
    }

    // --- FUNCTIONAL TESTS ---

    @Test
    @DisplayName("Функционал: Успешный LOGIN с валидным токеном и ключом")
    void endpoint_ValidLogin_ReturnsOk() throws Exception {
        String token = "1234567890ABCDEF1234567890ABCDEF";
        Allure.step("Настраиваем мок ActionService на успех для токена " + token);
        doNothing().when(actionService).auth(token);

        Allure.step("Отправляем запрос LOGIN");
        mockMvc.perform(post("/endpoint")
                .header("X-API-Key", "qazWSXedc")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", token)
                .param("action", "LOGIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(Result.OK.name()));
    }

    @Test
    @DisplayName("Функционал: Успешный ACTION с валидным токеном и ключом")
    void endpoint_ValidAction_ReturnsOk() throws Exception {
        String token = "1234567890ABCDEF1234567890ABCDEF";
        Allure.step("Настраиваем мок ActionService на успех для действия");
        doNothing().when(actionService).action(token);

        Allure.step("Отправляем запрос ACTION");
        mockMvc.perform(post("/endpoint")
                .header("X-API-Key", "qazWSXedc")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", token)
                .param("action", "ACTION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(Result.OK.name()));
    }

    @Test
    @DisplayName("Ошибки: Некорректный Action возвращает 400 Bad Request")
    void endpoint_InvalidAction_ReturnsBadRequest() throws Exception {
        Allure.step("Отправляем запрос с несуществующим действием UNKNOWN_ACTION");
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
    @DisplayName("Ошибки: Повторный LOGIN для существующего токена возвращает 409 Conflict")
    void endpoint_TokenAlreadyExists_ReturnsConflict() throws Exception {
        String token = "1234567890ABCDEF1234567890ABCDEF";
        Allure.step("Настраиваем мок на выброс исключения TokenAlreadyExists");
        doThrow(new recruitment.aqa.service.exceptions.TokenAlreadyExists(token))
                .when(actionService).auth(token);

        Allure.step("Отправляем LOGIN");
        mockMvc.perform(post("/endpoint")
                .header("X-API-Key", "qazWSXedc")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", token)
                .param("action", "LOGIN"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.result").value("ERROR"))
                .andExpect(jsonPath("$.message").value(containsString(token)));
    }

    @Test
    @DisplayName("Ошибки: Запрос ACTION для несуществующего токена возвращает 403 Forbidden")
    void endpoint_TokenNotFound_ReturnsForbidden() throws Exception {
        String token = "1234567890ABCDEF1234567890ABCDEF";
        Allure.step("Имитируем отсутствие токена в сервисе");
        doThrow(new recruitment.aqa.service.exceptions.TokenNotFound(token))
                .when(actionService).action(token);

        Allure.step("Отправляем ACTION");
        mockMvc.perform(post("/endpoint")
                .header("X-API-Key", "qazWSXedc")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", token)
                .param("action", "ACTION"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.result").value("ERROR"))
                .andExpect(jsonPath("$.message").value(containsString(token)));
    }

    @Test
    @DisplayName("Валидация: Пустой токен возвращает 400 Bad Request")
    void endpoint_ValidationError_ReturnsBadRequest() throws Exception {
        Allure.step("Отправляем запрос с пустым токеном");
        mockMvc.perform(post("/endpoint")
                .header("X-API-Key", "qazWSXedc")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", "")
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
    @DisplayName("Ошибки: Непредвиденное исключение возвращает 500 Internal Server Error")
    void endpoint_InternalError_Returns500() throws Exception {
        String token = "1234567890ABCDEF1234567890ABCDEF";
        Allure.step("Имитируем критический сбой в ActionService");
        doThrow(new RuntimeException("Something went wrong"))
                .when(actionService).logout(token);

        Allure.step("Отправляем запрос и проверяем 500 ошибку");
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
    @DisplayName("Функционал: Успешный LOGOUT")
    void endpoint_ValidLogout_ReturnsOk() throws Exception {
        String token = "1234567890ABCDEF1234567890ABCDEF";
        Allure.step("Имитируем успешное удаление токена");
        doNothing().when(actionService).logout(token);

        Allure.step("Отправляем LOGOUT");
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
