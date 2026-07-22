package su26.uml.be.config.anythingllm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import su26.uml.be.infrastructure.ai.AnythingLlmClient;
import su26.uml.be.infrastructure.ai.AnythingLlmProperties;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnythingLlmClientTest {

    @Mock
    WebClient anythingLlmWebClient;
    @Mock
    AnythingLlmProperties properties;
    AnythingLlmClient client;

    @BeforeEach
    void setUp() {
        lenient().when(properties.baseUrl()).thenReturn("http://localhost:3001/api");
        client = new AnythingLlmClient(anythingLlmWebClient, properties);
    }

    @Test
    void getSystemPreferences_returnsProviderList() {

        List<String> prefs = client.getSystemPreferences();

        assertNotNull(prefs);
        assertTrue(prefs.contains("ollama"));
        assertTrue(prefs.contains("groq"));
        assertTrue(prefs.contains("openai"));
        assertFalse(prefs.isEmpty());
    }
}
