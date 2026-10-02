package org.zayed.unirollweb.user;

import java.time.Instant;

// What the API shows about a user. Deliberately has no password hash.
public record UserResponse(Long id, String name, String email, Role role, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getCreatedAt());
    }
}
