package org.zayed.unirollweb.subject;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Used for both create (POST) and full update (PUT).
// Integer, not int: a missing field should fail @NotNull instead of silently becoming 0.
public record SubjectRequest(
        @NotBlank @Size(max = 10) @Pattern(regexp = "^\\s*[A-Za-z0-9]+\\s*$", message = "must contain only letters and digits")
        String code,
        @NotBlank @Size(max = 150) String name,
        @NotNull @Min(1) @Max(6) Integer creditHours,
        @NotNull @Min(1) @Max(1000) Integer capacity) {
}
