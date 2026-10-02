package org.zayed.unirollweb.common;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.zayed.unirollweb.user.Role;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/**
 * For @WebMvcTest: makes a MockMvc request look like it carries a valid JWT for the given user,
 * without creating or signing a real token. Usage: mockMvc.perform(get(...).with(lecturer(1)))
 */
public final class TestTokens {

    private TestTokens() {
    }

    public static RequestPostProcessor lecturer(long userId) {
        return tokenFor(userId, Role.LECTURER);
    }

    public static RequestPostProcessor student(long userId) {
        return tokenFor(userId, Role.STUDENT);
    }

    private static RequestPostProcessor tokenFor(long userId, Role role) {
        return jwt()
                .jwt(token -> token.subject(String.valueOf(userId)).claim("role", role.name()))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}
