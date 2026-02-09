package recruitment.aqa.service.services;

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
    @DisplayName("Auth: Should throw TokenAlreadyExists if token is in storage")
    void auth_TokenExists_ThrowsException() {
        String token = "existing-token";
        when(tokenStorage.contains(token)).thenReturn(true);

        assertThrows(TokenAlreadyExists.class, () -> actionService.auth(token));
        
        verify(tokenStorage, never()).put(anyString());
    }

    @Test
    @DisplayName("Auth: Should call external service and save token on success")
    void auth_Success_SavesToken() {
        String token = "new-token";
        when(tokenStorage.contains(token)).thenReturn(false);

        // Mocking RestClient chain
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(eq("/auth"), any(Object[].class))).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        
        actionService.auth(token);

        verify(tokenStorage).put(token);
    }
    
    @Test
    @DisplayName("Action: Should throw TokenNotFound if token is missing")
    void action_TokenMissing_ThrowsException() {
        String token = "missing-token";
        when(tokenStorage.contains(token)).thenReturn(false);

        assertThrows(TokenNotFound.class, () -> actionService.action(token));
    }

    @Test
    @DisplayName("Action: Should call external service if token exists")
    void action_Success_CallsExternal() {
        String token = "valid-token";
        when(tokenStorage.contains(token)).thenReturn(true);

        // Mocking RestClient chain
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(eq("/doAction"), any(Object[].class))).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);

        actionService.action(token);
    }

    @Test
    @DisplayName("Logout: Should remove token if exists")
    void logout_Success_RemovesToken() {
        String token = "valid-token";
        when(tokenStorage.remove(token)).thenReturn(true);

        actionService.logout(token);

        verify(tokenStorage).remove(token);
    }

    @Test
    @DisplayName("Logout: Should throw TokenNotFound if token missing")
    void logout_TokenMissing_ThrowsException() {
        String token = "missing-token";
        when(tokenStorage.remove(token)).thenReturn(false);

        assertThrows(TokenNotFound.class, () -> actionService.logout(token));
    }
}
