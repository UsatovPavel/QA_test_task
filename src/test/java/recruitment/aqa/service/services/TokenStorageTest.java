package recruitment.aqa.service.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenStorageTest {

    private final TokenStorage tokenStorage = new TokenStorage();

    /**
     * Verifies that a token can be added to the storage and then retrieved using contains().
     */
    @Test
    @DisplayName("Хранилище: Должно успешно сохранять токены")
    void put_ShouldAddToken() {
        String token = "token123";
        
        tokenStorage.put(token);
        assertTrue(tokenStorage.contains(token));
    }

    @Test
    @DisplayName("Хранилище: Должно проверять наличие токена")
    void contains_ShouldReturnTrueIfExists() {
        String token = "existingToken";
        tokenStorage.put(token);
        assertTrue(tokenStorage.contains(token));
        assertFalse(tokenStorage.contains("nonExistingToken"));
    }

    @Test
    @DisplayName("Хранилище: Должно удалять токены")
    void remove_ShouldRemoveToken() {
        String token = "tokenToRemove";
        tokenStorage.put(token);
        assertTrue(tokenStorage.remove(token));
        assertFalse(tokenStorage.contains(token));
        assertFalse(tokenStorage.remove("alreadyRemovedToken"));
    }
}
