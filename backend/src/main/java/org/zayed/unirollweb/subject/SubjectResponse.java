package org.zayed.unirollweb.subject;

import java.time.Instant;

public record SubjectResponse(
        Long id,
        String code,
        String name,
        int creditHours,
        int capacity,
        long enrolledCount,
        LecturerSummary lecturer,
        Instant createdAt) {

    // Only what a student needs to see about the lecturer; no email
    public record LecturerSummary(Long id, String name) {
    }

    public static SubjectResponse from(Subject subject, long enrolledCount) {
        return new SubjectResponse(
                subject.getId(),
                subject.getCode(),
                subject.getName(),
                subject.getCreditHours(),
                subject.getCapacity(),
                enrolledCount,
                new LecturerSummary(subject.getLecturer().getId(), subject.getLecturer().getName()),
                subject.getCreatedAt());
    }
}
