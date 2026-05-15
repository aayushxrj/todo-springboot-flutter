package com.aayushxrj.Todo.App.Permify;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class PermifyAuthorizationService {

    private static final String SYSTEM_ENTITY_TYPE = "system";
    private static final String SYSTEM_ROOT_ID = "root";

    private final PermifyClient permifyClient;

    public PermifyAuthorizationService(PermifyClient permifyClient) {
        this.permifyClient = permifyClient;
    }

    public void requireSystemPermission(String permission) {
        String userId = currentUserId();
        boolean allowed = permifyClient.checkPermission(userId, permission, SYSTEM_ENTITY_TYPE, SYSTEM_ROOT_ID);
        if (!allowed) {
            throw new AccessDeniedException("Forbidden");
        }
    }

    public void requireTaskPermission(String permission, String taskId) {
        String userId = currentUserId();
        boolean allowed = permifyClient.checkPermission(userId, permission, "task", taskId);
        if (!allowed) {
            throw new AccessDeniedException("Forbidden");
        }
    }

    private String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new AccessDeniedException("Unauthorized");
        }
        return authentication.getName();
    }
}
