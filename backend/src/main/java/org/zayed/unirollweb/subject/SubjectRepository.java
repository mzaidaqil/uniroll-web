package org.zayed.unirollweb.subject;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    boolean existsByCode(String code);
}
