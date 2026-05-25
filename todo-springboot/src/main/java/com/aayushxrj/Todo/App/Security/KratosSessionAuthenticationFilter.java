package com.aayushxrj.Todo.App.Security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class KratosSessionAuthenticationFilter extends OncePerRequestFilter {

    private static final String SESSION_HEADER = "X-Session-Token";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String whoamiUrl;

    public KratosSessionAuthenticationFilter(
            @Value("${kratos.public-base-url}") String kratosPublicBaseUrl
    ) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        String baseUrl = kratosPublicBaseUrl.endsWith("/")
                ? kratosPublicBaseUrl.substring(0, kratosPublicBaseUrl.length() - 1)
                : kratosPublicBaseUrl;
        this.whoamiUrl = baseUrl + "/sessions/whoami";
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator") || path.startsWith("/error");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String sessionToken = request.getHeader(SESSION_HEADER);
        String cookieHeader = request.getHeader(HttpHeaders.COOKIE);
        if ((sessionToken == null || sessionToken.isBlank())
                && (cookieHeader == null || cookieHeader.isBlank())) {
            filterChain.doFilter(request, response);
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (sessionToken != null && !sessionToken.isBlank()) {
            headers.set(SESSION_HEADER, sessionToken);
        } else {
            headers.set(HttpHeaders.COOKIE, cookieHeader);
        }

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<String> whoamiResponse;
        try {
            whoamiResponse = restTemplate.exchange(whoamiUrl, HttpMethod.GET, entity, String.class);
        } catch (RestClientResponseException ex) {
            filterChain.doFilter(request, response);
            return;
        }

        if (whoamiResponse.getStatusCode().is2xxSuccessful() && whoamiResponse.getBody() != null) {
            String principal = extractPrincipal(whoamiResponse.getBody());
            List<SimpleGrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_USER"),
                    new SimpleGrantedAuthority("ROLE_ADMIN")
            );
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    authorities
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String extractPrincipal(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode emailNode = root.path("identity").path("traits").path("email");
            if (emailNode.isTextual()) {
                return emailNode.asText();
            }
            JsonNode idNode = root.path("identity").path("id");
            if (idNode.isTextual()) {
                return idNode.asText();
            }
        } catch (IOException ignored) {
            return "kratos-user";
        }
        return "kratos-user";
    }
}
