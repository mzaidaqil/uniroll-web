package org.zayed.unirollweb.enrollment;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.zayed.unirollweb.TestcontainersConfiguration;
import org.zayed.unirollweb.subject.Subject;
import org.zayed.unirollweb.subject.SubjectRepository;
import org.zayed.unirollweb.user.Role;
import org.zayed.unirollweb.user.User;
import org.zayed.unirollweb.user.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class EnrollmentRepositoryTest {

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User student;
    private Subject programming;
    private Subject databases;

    @BeforeEach
    void createStudentAndSubjects() {
        User lecturer = userRepository.saveAndFlush(new User("Dr Tan", "tan@uni.edu", "hash", Role.LECTURER));
        student = userRepository.saveAndFlush(new User("Aisyah", "aisyah@uni.edu", "hash", Role.STUDENT));
        programming = subjectRepository.saveAndFlush(new Subject("CS101", "Programming", 3, 30, lecturer));
        databases = subjectRepository.saveAndFlush(new Subject("CS201", "Databases", 4, 30, lecturer));
    }

    @Test
    void savesEnrollmentAndChecksItExists() {
        Enrollment saved = enrollmentRepository.saveAndFlush(new Enrollment(student, programming));

        assertThat(saved.getEnrolledAt()).isNotNull();
        assertThat(enrollmentRepository.existsByStudentIdAndSubjectId(student.getId(), programming.getId())).isTrue();
        assertThat(enrollmentRepository.existsByStudentIdAndSubjectId(student.getId(), databases.getId())).isFalse();
    }

    @Test
    void rejectsEnrollingTwiceInSameSubject() {
        enrollmentRepository.saveAndFlush(new Enrollment(student, programming));

        assertThatThrownBy(() -> enrollmentRepository.saveAndFlush(new Enrollment(student, programming)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void countsEnrollmentsPerSubject() {
        User otherStudent = userRepository.saveAndFlush(new User("Ravi", "ravi@uni.edu", "hash", Role.STUDENT));
        enrollmentRepository.saveAndFlush(new Enrollment(student, programming));
        enrollmentRepository.saveAndFlush(new Enrollment(otherStudent, programming));

        assertThat(enrollmentRepository.countBySubjectId(programming.getId())).isEqualTo(2);
        assertThat(enrollmentRepository.countBySubjectId(databases.getId())).isZero();
    }

    @Test
    void sumsCreditHoursForStudent() {
        assertThat(enrollmentRepository.sumCreditHoursByStudentId(student.getId())).isZero();

        enrollmentRepository.saveAndFlush(new Enrollment(student, programming));
        enrollmentRepository.saveAndFlush(new Enrollment(student, databases));

        assertThat(enrollmentRepository.sumCreditHoursByStudentId(student.getId())).isEqualTo(7);
    }

    @Test
    void deletingSubjectDeletesItsEnrollments() {
        enrollmentRepository.saveAndFlush(new Enrollment(student, programming));
        entityManager.clear();

        subjectRepository.deleteById(programming.getId());
        subjectRepository.flush();
        entityManager.clear();

        assertThat(enrollmentRepository.count()).isZero();
    }
}
