package org.zayed.unirollweb.enrollment;

import org.zayed.unirollweb.user.User;

import java.time.Instant;

// One row of a lecturer's class list
public record EnrolledStudentResponse(Long studentId, String name, String email, Instant enrolledAt) {

    public static EnrolledStudentResponse from(Enrollment enrollment) {
        User student = enrollment.getStudent();
        return new EnrolledStudentResponse(student.getId(), student.getName(), student.getEmail(),
                enrollment.getEnrolledAt());
    }
}
