package recruitment.aqa.service.fuzzing;

import io.qameta.allure.Allure;
import net.jqwik.api.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import recruitment.aqa.service.Action;
import recruitment.aqa.service.configs.SecurityConfiguration;
import recruitment.aqa.service.controllers.ExceptionTranslator;
import recruitment.aqa.service.controllers.ResponseContextAdvice;
import recruitment.aqa.service.controllers.UserController;
import recruitment.aqa.service.services.ActionService;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({ExceptionTranslator.class, ResponseContextAdvice.class, SecurityConfiguration.class})
@TestPropertySource(properties = "secret=qazWSXedc")
class FuzzingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActionService actionService;

    // ==========================================
    // 1. TOKEN FUZZING
    // ==========================================

    @ParameterizedTest(name = "{index} ==> Ввод: ''{0}''")
    @DisplayName("Фаззинг токена (ASCII): Некорректный формат должен возвращать 400 Bad Request")
    @MethodSource("invalidTokensAscii")
    void fuzzToken_Ascii_ShouldReturnBadRequest(String invalidToken) throws Exception {
        System.out.println(">>> TESTING TOKEN (ASCII): [" + invalidToken + "]");
        Allure.step("Тестируем некорректный ASCII токен: [" + invalidToken + "]");
        mockMvc.perform(post("/endpoint")
                .header("X-API-Key", "qazWSXedc")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", invalidToken != null ? invalidToken : "")
                .param("action", "LOGIN"))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest(name = "{index} ==> Ввод: ''{0}''")
    @DisplayName("Фаззинг токена (Garbage): Любые некорректные данные должны возвращать 4xx")
    @MethodSource("invalidTokensGarbage")
    void fuzzToken_Garbage_ShouldReturn4xx(String invalidToken) throws Exception {
        System.out.println(">>> TESTING TOKEN (GARBAGE): [" + invalidToken + "]");
        Allure.step("Тестируем 'мусорный' токен: [" + invalidToken + "]");
        mockMvc.perform(post("/endpoint")
                .header("X-API-Key", "qazWSXedc")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", invalidToken != null ? invalidToken : "")
                .param("action", "LOGIN"))
                .andExpect(status().is4xxClientError());
    }

    static Stream<String> invalidTokensAscii() {
        Arbitrary<String> arb = Arbitraries.oneOf(
            // Too short (ASCII)
            Arbitraries.strings().alpha().numeric().ofMaxLength(31),
            // Too long (ASCII)
            Arbitraries.strings().alpha().numeric().ofMinLength(33).ofMaxLength(200),
            // Invalid chars (ASCII but not HEX) - e.g. non-hex letters
            Arbitraries.strings().alpha().filter(s -> !s.matches("^[0-9A-F]{32}$")),
            // Empty
            Arbitraries.just(""),
            // Null
            Arbitraries.just((String) null)
        );
        return Stream.generate(() -> arb.sample()).limit(200);
    }

    static Stream<String> invalidTokensGarbage() {
        Arbitrary<String> arb = Arbitraries.strings().all().ofMinLength(1);
        return Stream.generate(() -> arb.sample()).limit(200);
    }

    // ==========================================
    // 2. ACTION FUZZING
    // ==========================================

    @ParameterizedTest(name = "{index} ==> Ввод: ''{0}''")
    @DisplayName("Фаззинг Action (ASCII): Неверное значение enum должно возвращать 400")
    @MethodSource("invalidActionsAscii")
    void fuzzAction_Ascii_ShouldReturnBadRequest(String invalidAction) throws Exception {
        System.out.println(">>> TESTING ACTION (ASCII): [" + invalidAction + "]");
        Allure.step("Тестируем некорректный ASCII action: [" + invalidAction + "]");
        var request = post("/endpoint")
                .header("X-API-Key", "qazWSXedc")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", "1234567890ABCDEF1234567890ABCDEF");

        if (invalidAction != null) {
            request.param("action", invalidAction);
        }

        mockMvc.perform(request)
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest(name = "{index} ==> Ввод: ''{0}''")
    @DisplayName("Фаззинг Action (Garbage): Любые некорректные данные должны возвращать 4xx")
    @MethodSource("invalidActionsGarbage")
    void fuzzAction_Garbage_ShouldReturn4xx(String invalidAction) throws Exception {
        System.out.println(">>> TESTING ACTION (GARBAGE): [" + invalidAction + "]");
        Allure.step("Тестируем 'мусорный' action: [" + invalidAction + "]");
        var request = post("/endpoint")
                .header("X-API-Key", "qazWSXedc")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", "1234567890ABCDEF1234567890ABCDEF");

        if (invalidAction != null) {
            request.param("action", invalidAction);
        }

        mockMvc.perform(request)
                .andExpect(status().is4xxClientError());
    }

    static Stream<String> invalidActionsAscii() {
        Set<String> validActions = Arrays.stream(Action.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        Arbitrary<String> arb = Arbitraries.oneOf(
            // Random ASCII strings filtering out valid ones
            Arbitraries.strings().alpha().ofMinLength(1).filter(s -> !validActions.contains(s)),
            // Empty
            Arbitraries.just(""),
            // Null
            Arbitraries.just((String) null)
        );
        return Stream.generate(() -> arb.sample()).limit(200);
    }

    static Stream<String> invalidActionsGarbage() {
        return Stream.generate(() -> Arbitraries.strings().all().ofMinLength(1).sample()).limit(200);
    }

    // ==========================================
    // 3. API KEY FUZZING
    // ==========================================

    @ParameterizedTest(name = "{index} ==> Input: ''{0}''")
    @DisplayName("API Key (ASCII): Incorrect key should return 401 Unauthorized")
    @MethodSource("invalidApiKeysAscii")
    void fuzzApiKey_Ascii_ShouldReturnUnauthorized(String invalidKey) throws Exception {
        var request = post("/endpoint")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", "1234567890ABCDEF1234567890ABCDEF")
                .param("action", "LOGIN");

        if (invalidKey != null) {
            request.header("X-API-Key", invalidKey);
        }

        mockMvc.perform(request)
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest(name = "{index} ==> Input: ''{0}''")
    @DisplayName("API Key (Garbage): Malformed headers should return 4xx (Client Error)")
    @MethodSource("invalidApiKeysGarbage")
    void fuzzApiKey_Garbage_ShouldReturn4xx(String invalidKey) throws Exception {
        var request = post("/endpoint")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", "1234567890ABCDEF1234567890ABCDEF")
                .param("action", "LOGIN");

        if (invalidKey != null) {
            request.header("X-API-Key", invalidKey);
        }

        mockMvc.perform(request)
                .andExpect(status().is4xxClientError());
    }

    static Stream<String> invalidApiKeysAscii() {
        String correctKey = "qazWSXedc";
        Arbitrary<String> arb = Arbitraries.oneOf(
            // Random ASCII strings (excluding correct key)
            Arbitraries.strings().alpha().numeric().ofMinLength(1).filter(s -> !s.equals(correctKey)),
            // Empty
            Arbitraries.just(""),
            // Null
            Arbitraries.just((String) null)
        );
        return Stream.generate(() -> arb.sample()).limit(200);
    }

    static Stream<String> invalidApiKeysGarbage() {
        return Stream.generate(() -> Arbitraries.strings().all().ofMinLength(1).sample()).limit(200);
    }
}
