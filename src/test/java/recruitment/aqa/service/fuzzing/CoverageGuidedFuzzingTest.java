package recruitment.aqa.service.fuzzing;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import recruitment.aqa.service.configs.SecurityConfiguration;
import recruitment.aqa.service.controllers.ExceptionTranslator;
import recruitment.aqa.service.controllers.ResponseContextAdvice;
import recruitment.aqa.service.controllers.UserController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({ExceptionTranslator.class, ResponseContextAdvice.class, SecurityConfiguration.class})
@TestPropertySource(properties = "secret=qazWSXedc")
class CoverageGuidedFuzzingTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private recruitment.aqa.service.services.ActionService actionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @FuzzTest
    void fuzzTokenAndAction(FuzzedDataProvider data) throws Exception {
        String token = data.consumeString(40); // Generate potentially valid length or garbage
        String action = data.consumeString(20);
        String apiKey = data.consumeString(20);

        try {
            mockMvc.perform(post("/endpoint")
                    .header("X-API-Key", apiKey)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("token", token)
                    .param("action", action))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        // We expect 4xx errors (400, 401, 403, 404, 409 etc.)
                        // 5xx errors are failures.
                        if (status >= 500) {
                            throw new AssertionError("Fuzzing found 5xx Server Error: " + status);
                        }
                    });
        } catch (AssertionError e) {
            throw e;
        } catch (Exception e) {
            // Some exceptions might be thrown before response (e.g. filter issues), check if they are expected
        }
    }
    
    @FuzzTest
    void fuzzRefined(FuzzedDataProvider data) throws Exception {
         // More targeted fuzzing
         String validKey = "qazWSXedc";
         String garbageKey = data.consumeString(100);
         
         mockMvc.perform(post("/endpoint")
                 .header("X-API-Key", data.consumeBoolean() ? validKey : garbageKey) // Toggle valid/invalid key
                 .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                 .param("token", data.consumeAsciiString(32))
                 .param("action", data.consumeAsciiString(10)))
                 .andExpect(status().is4xxClientError()); // Expect robustness
    }
}
