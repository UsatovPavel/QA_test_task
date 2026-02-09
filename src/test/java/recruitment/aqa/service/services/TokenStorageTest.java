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
    @DisplayName("should be able to put tokens")
    void put_ShouldAddToken() {
        String token = "token123";
        
        tokenStorage.put(token);
        assertTrue(tokenStorage.contains(token));
    }

    @Test
    @DisplayName("should check if token exists")
    void contains_ShouldReturnTrueIfExists() {
        String token = "existingToken";
        tokenStorage.put(token);
        assertTrue(tokenStorage.contains(token));
        assertFalse(tokenStorage.contains("nonExistingToken"));
    }

    @Test
    @DisplayName("should remove token")
    void remove_ShouldRemoveToken() {
        String token = "tokenToRemove";
        tokenStorage.put(token);
        assertTrue(tokenStorage.remove(token));
        assertFalse(tokenStorage.contains(token));
        assertFalse(tokenStorage.remove("alreadyRemovedToken"));
    }
}
