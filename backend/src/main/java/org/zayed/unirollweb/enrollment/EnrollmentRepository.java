package org.zayed.unirollweb.enrollment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    boolean existsByStudentIdAndSubjectId(Long studentId, Long subjectId);

    // Seats taken, for the capacity check
    long countBySubjectId(Long subjectId);

    // Credit hours a student is already taking, for the 20-hour cap; 0 when not enrolled in anything
    @Query("""
            SELECT COALESCE(SUM(e.subject.creditHours), 0)
            FROM Enrollment e
            WHERE e.student.id = :studentId
            """)
    long sumCreditHoursByStudentId(@Param("studentId") Long studentId);
}
