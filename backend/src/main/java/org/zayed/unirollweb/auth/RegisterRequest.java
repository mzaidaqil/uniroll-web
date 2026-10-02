package org.zayed.unirollweb.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.zayed.unirollweb.user.Role;

public record RegisterRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Email @Size(max = 255) String email,
        // BCrypt only uses the first 72 bytes of a password, so longer ones are rejected rather than silently cut
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotNull Role role) {
}
