package org.comroid.auth.model;

public record AuthorizationRequest(String clientId, String sessionId, String redirectUri, String externalForm) {
}
