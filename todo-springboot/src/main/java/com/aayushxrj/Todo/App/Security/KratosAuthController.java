package com.aayushxrj.Todo.App.Security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth/kratos")
public class KratosAuthController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String kratosPublicBaseUrl;

    public KratosAuthController(
            @Value("${kratos.public-base-url}") String kratosPublicBaseUrl
    ) {
        this.kratosPublicBaseUrl = kratosPublicBaseUrl.endsWith("/")
                ? kratosPublicBaseUrl.substring(0, kratosPublicBaseUrl.length() - 1)
                : kratosPublicBaseUrl;
    }

    @PostMapping("/login")
    public ResponseEntity<KratosSessionResponse> login(@Valid @RequestBody KratosLoginRequest request) {
        String flowBody = startFlow("login");
        if (flowBody == null) {
            return ResponseEntity.status(502).build();
        }

        String actionUrl = extractAction(flowBody, "login");
        actionUrl = normalizeActionUrl(actionUrl, "login");
        Map<String, Object> payload = new HashMap<>();
        payload.put("method", "password");
        payload.put("identifier", request.getEmail());
        payload.put("password", request.getPassword());

        ResponseEntity<String> loginResponse = postJson(actionUrl, payload);
        if (!loginResponse.getStatusCode().is2xxSuccessful() || loginResponse.getBody() == null) {
            return ResponseEntity.status(loginResponse.getStatusCode()).build();
        }

        String sessionToken = extractSessionToken(loginResponse.getBody());
        if (sessionToken.isEmpty()) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(new KratosSessionResponse(sessionToken));
    }

    @PostMapping("/register")
    public ResponseEntity<KratosSessionResponse> register(@Valid @RequestBody KratosRegisterRequest request) {
        String flowBody = startFlow("registration");
        if (flowBody == null) {
            return ResponseEntity.status(502).build();
        }

        String actionUrl = extractAction(flowBody, "registration");
        actionUrl = normalizeActionUrl(actionUrl, "registration");
        Map<String, Object> traits = new HashMap<>();
        traits.put("email", request.getEmail());
        traits.put("name", Map.of("first", request.getFirstName(), "last", request.getLastName()));

        Map<String, Object> payload = new HashMap<>();
        payload.put("method", "password");
        payload.put("password", request.getPassword());
        payload.put("traits", traits);

        ResponseEntity<String> registerResponse = postJson(actionUrl, payload);
        if (!registerResponse.getStatusCode().is2xxSuccessful() || registerResponse.getBody() == null) {
            return ResponseEntity.status(registerResponse.getStatusCode()).build();
        }

        String sessionToken = extractSessionToken(registerResponse.getBody());
        return ResponseEntity.ok(new KratosSessionResponse(sessionToken));
    }

    private String startFlow(String flowType) {
        String flowUrl = kratosPublicBaseUrl + "/self-service/" + flowType + "/api?refresh=true";
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(flowUrl, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }
        } catch (RestClientResponseException ex) {
            return null;
        }
        return null;
    }

    private String extractAction(String flowBody, String flowType) {
        try {
            JsonNode root = objectMapper.readTree(flowBody);
            JsonNode action = root.path("ui").path("action");
            if (action.isTextual()) {
                return action.asText();
            }
            JsonNode id = root.path("id");
            if (id.isTextual()) {
                return kratosPublicBaseUrl + "/self-service/" + flowType + "?flow=" + id.asText();
            }
        } catch (IOException ignored) {
            return kratosPublicBaseUrl + "/self-service/" + flowType;
        }
        return kratosPublicBaseUrl + "/self-service/" + flowType;
    }

    private String normalizeActionUrl(String actionUrl, String flowType) {
        if (actionUrl == null || actionUrl.isBlank()) {
            return kratosPublicBaseUrl + "/self-service/" + flowType;
        }

        if (actionUrl.startsWith("/")) {
            return kratosPublicBaseUrl + actionUrl;
        }

        try {
            URI uri = new URI(actionUrl);
            String host = uri.getHost();
            if (host == null) {
                return kratosPublicBaseUrl + "/self-service/" + flowType;
            }
            if ("localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host)) {
                return kratosPublicBaseUrl + uri.getPath() + (uri.getQuery() == null ? "" : "?" + uri.getQuery());
            }
        } catch (URISyntaxException ignored) {
            return kratosPublicBaseUrl + "/self-service/" + flowType;
        }

        return actionUrl;
    }

    private ResponseEntity<String> postJson(String url, Map<String, Object> payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        return restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
    }

    private String extractSessionToken(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode token = root.path("session_token");
            if (token.isTextual()) {
                return token.asText();
            }
        } catch (IOException ignored) {
            return "";
        }
        return "";
    }
}
