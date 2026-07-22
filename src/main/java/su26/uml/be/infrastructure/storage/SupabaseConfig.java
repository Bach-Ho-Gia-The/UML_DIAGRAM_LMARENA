package su26.uml.be.infrastructure.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Wires the Supabase Storage {@link WebClient}.
 * <p>
 * Base URL points at the Storage REST API ({@code {SUPABASE_URL}/storage/v1}) and the
 * {@code service_role} key is attached as a default {@code Authorization}/{@code apikey} header on every
 * request. The service key has full bucket access and MUST stay on the backend — it is never returned to
 * the frontend.
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(SupabaseProperties.class)
public class SupabaseConfig {

    private final SupabaseProperties supabaseProperties;

    /** Allow a 16 MB in-memory buffer so 10 MB PDF uploads are never truncated by the default 256 KB codec limit. */
    private static final int MAX_IN_MEMORY_SIZE = 16 * 1024 * 1024;

    @Bean
    public WebClient supabaseWebClient() {
        // Use the static WebClient.builder() factory instead of injecting the auto-configured
        // WebClient.Builder bean. This only needs spring-webflux on the classpath (it is) and does
        // not depend on Boot's WebClientAutoConfiguration / web-application-type, so it can never
        // fail with "required a bean of type WebClient$Builder that could not be found".
        return WebClient.builder()
                .baseUrl(supabaseProperties.url() + "/storage/v1")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + supabaseProperties.serviceKey())
                .defaultHeader("apikey", supabaseProperties.serviceKey())
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_SIZE))
                .build();
    }
}