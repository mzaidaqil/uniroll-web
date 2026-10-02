package org.zayed.unirollweb.common;

import org.springframework.security.oauth2.jwt.Jwt;

public final class CurrentUser {

    private CurrentUser() {
    }

    // The token's "sub" claim is the user id (see JwtService). Spring has already verified the signature.
    public static Long id(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
