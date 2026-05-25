package com.aayushxrj.Todo.App.TodosList;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class HydraTokenService {

    private static final long CLOCK_SKEW_SECONDS = 30;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String tokenUrl;
    private final String clientId;
    private final String clientSecret;
    private final String scope;
    private final String audience;

    private final AtomicReference<TokenState> tokenState = new AtomicReference<>(TokenState.empty());

    public HydraTokenService(
            @Value("${hydra.public-base-url}") String hydraPublicBaseUrl,
            @Value("${hydra.client-id}") String clientId,
            @Value("${hydra.client-secret}") String clientSecret,
            @Value("${hydra.scope:}") String scope,
            @Value("${hydra.audience:}") String audience
    ) {
        String baseUrl = hydraPublicBaseUrl.endsWith("/")
                ? hydraPublicBaseUrl.substring(0, hydraPublicBaseUrl.length() - 1)
                : hydraPublicBaseUrl;
        this.tokenUrl = baseUrl + "/oauth2/token";
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.scope = scope == null ? "" : scope;
        this.audience = audience == null ? "" : audience;
    }

    public String getAccessToken() {
        TokenState current = tokenState.get();
        if (current.isValid()) {
            return current.token();
        }
        synchronized (this) {
            TokenState refreshed = tokenState.get();
            if (refreshed.isValid()) {
                return refreshed.token();
            }
            TokenState newState = fetchToken();
            tokenState.set(newState);
            return newState.token();
        }
    }

    private TokenState fetchToken() {
        String basic = Base64.getEncoder()
                .encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + basic);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        StringBuilder body = new StringBuilder("grant_type=client_credentials");
        if (!scope.isBlank()) {
            body.append("&scope=").append(scope);
        }
        if (!audience.isBlank()) {
            body.append("&audience=").append(audience);
        }

        HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);
        try {
            String response = restTemplate.postForObject(tokenUrl, entity, String.class);
            JsonNode root = objectMapper.readTree(Objects.requireNonNull(response));
            String accessToken = root.path("access_token").asText("");
            long expiresIn = root.path("expires_in").asLong(0);
            if (accessToken.isEmpty()) {
                throw new IllegalStateException("Hydra token response missing access_token");
            }
            Instant expiresAt = Instant.now().plusSeconds(Math.max(0, expiresIn - CLOCK_SKEW_SECONDS));
            return new TokenState(accessToken, expiresAt);
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException(
                    "Hydra token request failed: " + ex.getStatusCode().value(),
                    ex
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Hydra token request failed", ex);
        }
    }

    private record TokenState(String token, Instant expiresAt) {
        static TokenState empty() {
            return new TokenState("", Instant.EPOCH);
        }

        boolean isValid() {
            return token != null && !token.isBlank() && expiresAt.isAfter(Instant.now());
        }
    }
}
