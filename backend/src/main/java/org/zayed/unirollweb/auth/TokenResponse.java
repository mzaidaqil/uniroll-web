package org.zayed.unirollweb.auth;

// tokenType "Bearer" tells the client to send: Authorization: Bearer <accessToken>
public record TokenResponse(String accessToken, String tokenType, long expiresInSeconds) {
}
