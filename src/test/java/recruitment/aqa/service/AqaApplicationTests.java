package recruitment.aqa.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = "mock=http://localhost:8080")
class AqaApplicationTests {

    @Test
    void contextLoads() {
        // This test ensures that the Spring context loads successfully.
        // It covers configuration classes and checks for circular dependencies.
    }
}
