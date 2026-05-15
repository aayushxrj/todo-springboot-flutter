package com.aayushxrj.Todo.App.Permify;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PermifyClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String tenantId;

    public PermifyClient(
            @Value("${permify.base-url}") String baseUrl,
            @Value("${permify.tenant-id}") String tenantId
    ) {
        this.restTemplate = new RestTemplate();
        this.baseUrl = baseUrl;
        this.tenantId = tenantId;
    }

    public void writeSchema(String schema) {
        Map<String, Object> body = new HashMap<>();
        body.put("schema", schema);
        post("/v1/tenants/%s/schemas/write".formatted(tenantId), body);
    }

    public void writeTuples(List<Map<String, Object>> tuples) {
        Map<String, Object> body = new HashMap<>();
        body.put("metadata", Map.of("schema_version", ""));
        body.put("tuples", tuples);
        post("/v1/tenants/%s/data/write".formatted(tenantId), body);
    }

    public boolean checkPermission(String subjectId, String permission, String entityType, String entityId) {
        Map<String, Object> body = new HashMap<>();
        body.put("metadata", Map.of("snap_token", "", "schema_version", "", "depth", 20));
        body.put("entity", Map.of("type", entityType, "id", entityId));
        body.put("permission", permission);
        body.put("subject", Map.of("type", "user", "id", subjectId, "relation", ""));

        Map<String, Object> response = post("/v1/tenants/%s/permissions/check".formatted(tenantId), body);
        Object can = response.get("can");
        return "CHECK_RESULT_ALLOWED".equals(can);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> post(String path, Map<String, Object> body) {
        String url = baseUrl + path;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        return restTemplate.postForObject(url, entity, Map.class);
    }
}
