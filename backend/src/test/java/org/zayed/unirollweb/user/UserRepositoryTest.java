package org.zayed.unirollweb.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.zayed.unirollweb.TestcontainersConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void savesUserAndFindsByEmail() {
        userRepository.saveAndFlush(new User("Aisyah", "aisyah@uni.edu", "hash", Role.STUDENT));

        assertThat(userRepository.findByEmail("aisyah@uni.edu"))
                .hasValueSatisfying(user -> {
                    assertThat(user.getId()).isNotNull();
                    assertThat(user.getRole()).isEqualTo(Role.STUDENT);
                    assertThat(user.getCreatedAt()).isNotNull();
                });
        assertThat(userRepository.existsByEmail("nobody@uni.edu")).isFalse();
    }

    @Test
    void storesEmailInLowercase() {
        userRepository.saveAndFlush(new User("Aisyah", "  Aisyah@Uni.EDU ", "hash", Role.STUDENT));

        assertThat(userRepository.existsByEmail("aisyah@uni.edu")).isTrue();
    }

    @Test
    void rejectsDuplicateEmailIgnoringCase() {
        userRepository.saveAndFlush(new User("Aisyah", "aisyah@uni.edu", "hash", Role.STUDENT));

        assertThatThrownBy(() -> userRepository.saveAndFlush(
                new User("Other", "AISYAH@uni.edu", "hash", Role.STUDENT)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
