package org.zayed.unirollweb.subject;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.zayed.unirollweb.TestcontainersConfiguration;
import org.zayed.unirollweb.user.Role;
import org.zayed.unirollweb.user.User;
import org.zayed.unirollweb.user.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class SubjectRepositoryTest {

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User lecturer;

    @BeforeEach
    void createLecturer() {
        lecturer = userRepository.saveAndFlush(new User("Dr Tan", "tan@uni.edu", "hash", Role.LECTURER));
    }

    @Test
    void savesSubjectAndChecksCodeExists() {
        Subject saved = subjectRepository.saveAndFlush(new Subject("CS101", "Programming", 3, 30, lecturer));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(subjectRepository.existsByCode("CS101")).isTrue();
        assertThat(subjectRepository.existsByCode("CS999")).isFalse();
    }

    @Test
    void rejectsDuplicateCode() {
        subjectRepository.saveAndFlush(new Subject("CS101", "Programming", 3, 30, lecturer));

        assertThatThrownBy(() -> subjectRepository.saveAndFlush(
                new Subject("CS101", "Another Programming", 3, 30, lecturer)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 7})
    void rejectsCreditHoursOutsideOneToSix(int creditHours) {
        assertThatThrownBy(() -> subjectRepository.saveAndFlush(
                new Subject("CS102", "Programming", creditHours, 30, lecturer)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsZeroCapacity() {
        assertThatThrownBy(() -> subjectRepository.saveAndFlush(
                new Subject("CS103", "Programming", 3, 0, lecturer)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletingLecturerDeletesTheirSubjects() {
        subjectRepository.saveAndFlush(new Subject("CS101", "Programming", 3, 30, lecturer));
        entityManager.clear();

        userRepository.deleteById(lecturer.getId());
        userRepository.flush();
        entityManager.clear();

        assertThat(subjectRepository.count()).isZero();
    }
}
