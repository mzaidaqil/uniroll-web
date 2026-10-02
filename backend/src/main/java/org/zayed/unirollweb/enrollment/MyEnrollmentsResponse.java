package org.zayed.unirollweb.enrollment;

import java.util.List;

// GET /api/enrollments/me: the list plus the totals a student dashboard needs
public record MyEnrollmentsResponse(int totalCreditHours, int maxCreditHours, List<EnrollmentResponse> enrollments) {
}
