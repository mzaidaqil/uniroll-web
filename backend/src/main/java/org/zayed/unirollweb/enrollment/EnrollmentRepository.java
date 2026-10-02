package org.zayed.unirollweb.enrollment;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    boolean existsByStudentIdAndSubjectId(Long studentId, Long subjectId);

    Optional<Enrollment> findByStudentIdAndSubjectId(Long studentId, Long subjectId);

    // A student's timetable: subject and its lecturer loaded in the same query
    @EntityGraph(attributePaths = {"subject", "subject.lecturer"})
    List<Enrollment> findByStudentIdOrderBySubjectCodeAsc(Long studentId);

    // A subject's class list, earliest enrollment first
    @EntityGraph(attributePaths = "student")
    List<Enrollment> findBySubjectIdOrderByEnrolledAtAsc(Long subjectId);

    // Seats taken, for the capacity check
    long countBySubjectId(Long subjectId);

    // Credit hours a student is already taking, for the 20-hour cap; 0 when not enrolled in anything
    @Query("""
            SELECT COALESCE(SUM(e.subject.creditHours), 0)
            FROM Enrollment e
            WHERE e.student.id = :studentId
            """)
    long sumCreditHoursByStudentId(@Param("studentId") Long studentId);

    // Seats taken for many subjects in one query (subjects with no enrollments are simply absent)
    @Query("""
            SELECT e.subject.id AS subjectId, COUNT(e) AS enrolled
            FROM Enrollment e
            WHERE e.subject.id IN :subjectIds
            GROUP BY e.subject.id
            """)
    List<SubjectEnrollmentCount> countBySubjectIds(@Param("subjectIds") Collection<Long> subjectIds);

    // Spring Data fills this from the query's column aliases (subjectId, enrolled)
    interface SubjectEnrollmentCount {
        Long getSubjectId();

        long getEnrolled();
    }
}
