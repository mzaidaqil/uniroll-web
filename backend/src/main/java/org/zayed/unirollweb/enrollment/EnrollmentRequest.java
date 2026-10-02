package org.zayed.unirollweb.enrollment;

import jakarta.validation.constraints.NotNull;

// No studentId field: the student is always the logged-in user, taken from the token
public record EnrollmentRequest(@NotNull Long subjectId) {
}
