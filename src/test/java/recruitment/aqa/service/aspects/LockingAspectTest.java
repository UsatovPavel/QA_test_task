package recruitment.aqa.service.aspects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;
import recruitment.aqa.service.services.ActionService;
import recruitment.aqa.service.services.TokenStorage;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@TestPropertySource(properties = "mock=http://localhost:8080")
class LockingAspectTest {

    @Autowired
    private ActionService actionService;

    @MockitoBean
    private RestClient restClient;

    @MockitoBean
    private TokenStorage tokenStorage;

    @Test
    @DisplayName("Aspect Execution: Should execute locked method successfully and update MDC")
    void shouldExecuteLockedMethod() {
        MDC.clear(); // Ensure clean state
        String token = "testToken";
        when(tokenStorage.contains(token)).thenReturn(true);
        
        // Mocking RestClient request chain
        RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(any(String.class), any(Object[].class))).thenReturn(bodySpec);
        when(bodySpec.body(any(Object.class))).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);

        // Aspect logic: puts token into MDC
        assertDoesNotThrow(() -> actionService.action(token));
        
        // Verify Aspect execution side-effect
        assertEquals(token, MDC.get("token"), "Aspect should have put token into MDC");
    }

    @Test
    @DisplayName("Aspect: Should throw exception when key is not found in parameters")
    void shouldThrowExceptionWhenKeyNotFound() throws Throwable {
        // Given
        LockingAspect aspect = new LockingAspect(null);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        Lock annotation = mock(Lock.class);

        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"someValue"});
        when(signature.getParameterNames()).thenReturn(new String[]{"otherParam"});
        when(signature.getMethod()).thenReturn(this.getClass().getDeclaredMethods()[0]); // Just any method
        when(annotation.key()).thenReturn("targetKey");

        // When & Then
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> {
            aspect.lock(joinPoint, annotation);
        });
    }

    @Test
    @DisplayName("Aspect: Should throw exception when matching argument is not a String")
    void shouldThrowExceptionWhenArgumentIsNotString() throws Throwable {
        // Given
        LockingAspect aspect = new LockingAspect(null);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        Lock annotation = mock(Lock.class);

        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{123}); // Integer, not String
        when(signature.getParameterNames()).thenReturn(new String[]{"targetKey"});
        when(signature.getMethod()).thenReturn(this.getClass().getDeclaredMethods()[0]);
        when(annotation.key()).thenReturn("targetKey");

        // When & Then
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> {
            aspect.lock(joinPoint, annotation);
        });
    }
}
