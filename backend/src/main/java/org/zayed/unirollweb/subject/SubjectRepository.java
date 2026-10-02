package org.zayed.unirollweb.subject;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    boolean existsByCode(String code);

    // For updates: is the new code taken by a different subject?
    boolean existsByCodeAndIdNot(String code, Long id);

    // Case-insensitive match on code or name. The entity graph loads each subject's lecturer
    // in the same query, instead of one extra query per subject (the N+1 problem).
    @EntityGraph(attributePaths = "lecturer")
    @Query("""
            SELECT s FROM Subject s
            WHERE LOWER(s.code) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%'))
            """)
    Page<Subject> search(@Param("search") String search, Pageable pageable);

    // Same search, limited to one lecturer's subjects (the lecturer's "My subjects" page)
    @EntityGraph(attributePaths = "lecturer")
    @Query("""
            SELECT s FROM Subject s
            WHERE s.lecturer.id = :lecturerId
              AND (LOWER(s.code) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Subject> searchByLecturer(@Param("search") String search, @Param("lecturerId") Long lecturerId,
                                   Pageable pageable);

    // SELECT ... FOR UPDATE: other transactions that want this row wait until we commit.
    // Used wherever capacity is checked, so two requests can't both take the last seat.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Subject s WHERE s.id = :id")
    Optional<Subject> findByIdForUpdate(@Param("id") Long id);
}
