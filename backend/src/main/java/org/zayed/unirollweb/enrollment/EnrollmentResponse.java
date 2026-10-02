package org.zayed.unirollweb.enrollment;

import org.zayed.unirollweb.subject.Subject;

import java.time.Instant;

// One row of a student's timetable
public record EnrollmentResponse(
        Long subjectId,
        String subjectCode,
        String subjectName,
        int creditHours,
        String lecturerName,
        Instant enrolledAt) {

    public static EnrollmentResponse from(Enrollment enrollment) {
        Subject subject = enrollment.getSubject();
        return new EnrollmentResponse(
                subject.getId(),
                subject.getCode(),
                subject.getName(),
                subject.getCreditHours(),
                subject.getLecturer().getName(),
                enrollment.getEnrolledAt());
    }
}
