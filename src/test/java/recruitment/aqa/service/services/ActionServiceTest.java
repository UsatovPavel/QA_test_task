package recruitment.aqa.service.services;

import io.qameta.allure.Allure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import recruitment.aqa.service.exceptions.TokenAlreadyExists;
import recruitment.aqa.service.exceptions.TokenNotFound;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActionServiceTest {

    @Mock
    private RestClient restClient;

    @Mock
    private TokenStorage tokenStorage;

    @InjectMocks
    private ActionService actionService;

    /**
     * Mocks for the fluent API of RestClient.
     * Required to mock the chain: .post().uri().body().retrieve().toBodilessEntity()
     */
    
    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock
    private RestClient.RequestBodySpec requestBodySpec;
    @Mock
    private RestClient.ResponseSpec responseSpec;

    @BeforeEach
    void setUp() {
        // Common Mocks configuration could go here if needed
    }

    @Test
    @DisplayName("Auth: Должен выбрасывать TokenAlreadyExists, если токен уже есть в хранилище")
    void auth_TokenExists_ThrowsException() {
        String token = "existing-token";
        Allure.step("Настраиваем хранилище: токен уже существует");
        when(tokenStorage.contains(token)).thenReturn(true);

        Allure.step("Проверяем, что запрос на AUTH выбрасывает исключение TokenAlreadyExists");
        assertThrows(TokenAlreadyExists.class, () -> actionService.auth(token));
        
        Allure.step("Проверяем, что новый токен НЕ был сохранен");
        verify(tokenStorage, never()).put(anyString());
    }

    @Test
    @DisplayName("Auth: Должен вызывать внешний сервис и сохранять токен при успехе")
    void auth_Success_SavesToken() {
        String token = "new-token";
        Allure.step("Настраиваем окружение для нового токена");
        when(tokenStorage.contains(token)).thenReturn(false);

        Allure.step("Мокаем цепочку вызова RestClient для /auth");
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(eq("/auth"), any(Object[].class))).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        
        Allure.step("Выполняем AUTH");
        actionService.auth(token);

        Allure.step("Проверяем, что токен был сохранен в локальное хранилище");
        verify(tokenStorage).put(token);
    }
    
    @Test
    @DisplayName("Action: Должен выбрасывать TokenNotFound, если токен отсутствует")
    void action_TokenMissing_ThrowsException() {
        String token = "missing-token";
        Allure.step("Настраиваем хранилище: токен отсутствует");
        when(tokenStorage.contains(token)).thenReturn(false);

        Allure.step("Проверяем исключение TokenNotFound");
        assertThrows(TokenNotFound.class, () -> actionService.action(token));
    }

    @Test
    @DisplayName("Action: Должен вызывать внешний сервис, если токен существует")
    void action_Success_CallsExternal() {
        String token = "valid-token";
        Allure.step("Настраиваем хранилище: токен существует");
        when(tokenStorage.contains(token)).thenReturn(true);

        Allure.step("Мокаем цепочку вызова RestClient для /doAction");
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(eq("/doAction"), any(Object[].class))).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);

        Allure.step("Выполняем ACTION");
        actionService.action(token);
    }

    @Test
    @DisplayName("Logout: Должен удалять токен, если он существует")
    void logout_Success_RemovesToken() {
        String token = "valid-token";
        Allure.step("Имитируем наличие и успешное удаление токена в хранилище");
        when(tokenStorage.remove(token)).thenReturn(true);

        Allure.step("Выполняем LOGOUT");
        actionService.logout(token);

        Allure.step("Проверяем вызов метода удаления");
        verify(tokenStorage).remove(token);
    }

    @Test
    @DisplayName("Logout: Должен выбрасывать TokenNotFound, если токен отсутствует")
    void logout_TokenMissing_ThrowsException() {
        String token = "missing-token";
        Allure.step("Имитируем отсутствие токена при удалении");
        when(tokenStorage.remove(token)).thenReturn(false);

        Allure.step("Проверяем исключение TokenNotFound");
        assertThrows(TokenNotFound.class, () -> actionService.logout(token));
    }
}
