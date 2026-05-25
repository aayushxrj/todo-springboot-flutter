package com.aayushxrj.Todo.App.Security;

public class KratosSessionResponse {

    private final String sessionToken;

    public KratosSessionResponse(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public String getSessionToken() {
        return sessionToken;
    }
}
