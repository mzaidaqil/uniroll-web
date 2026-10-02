package org.zayed.unirollweb.enrollment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.zayed.unirollweb.TestcontainersConfiguration;
import org.zayed.unirollweb.auth.JwtService;
import org.zayed.unirollweb.common.ConflictException;
import org.zayed.unirollweb.subject.Subject;
import org.zayed.unirollweb.subject.SubjectRepository;
import org.zayed.unirollweb.user.Role;
import org.zayed.unirollweb.user.User;
import org.zayed.unirollweb.user.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Enrollment rules over HTTP against a real database. Data is not rolled back between tests,
 * so each test creates its own users and subjects.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class EnrollmentIntegrationTest {

    private static final AtomicInteger codeCounter = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private JwtService jwtService;

    @Test
    void studentEnrollsAndSeesSubjectInTimetable() throws Exception {
        Subject subject = subject(lecturer(), 3, 30);
        User student = user(Role.STUDENT);

        enroll(student, subject)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subjectCode").value(subject.getCode()))
                .andExpect(jsonPath("$.lecturerName").value("Dr Tan"));

        mockMvc.perform(get("/api/enrollments/me").header("Authorization", bearer(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCreditHours").value(3))
                .andExpect(jsonPath("$.maxCreditHours").value(20))
                .andExpect(jsonPath("$.enrollments.length()").value(1))
                .andExpect(jsonPath("$.enrollments[0].subjectId").value(subject.getId()));
        mockMvc.perform(get("/api/subjects/" + subject.getId()).header("Authorization", bearer(student)))
                .andExpect(jsonPath("$.enrolledCount").value(1));
    }

    @Test
    void cannotEnrollTwiceInSameSubject() throws Exception {
        Subject subject = subject(lecturer(), 3, 30);
        User student = user(Role.STUDENT);
        enroll(student, subject).andExpect(status().isCreated());

        enroll(student, subject).andExpect(status().isConflict());
    }

    @Test
    void cannotEnrollInFullSubject() throws Exception {
        Subject subject = subject(lecturer(), 3, 1);
        enroll(user(Role.STUDENT), subject).andExpect(status().isCreated());

        enroll(user(Role.STUDENT), subject)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(subject.getCode() + " is full"));
    }

    @Test
    void cannotGoAbove20CreditHours() throws Exception {
        User lecturer = lecturer();
        User student = user(Role.STUDENT);
        for (int i = 0; i < 3; i++) {
            enroll(student, subject(lecturer, 6, 30)).andExpect(status().isCreated());
        }

        // 18 + 3 = 21: rejected
        enroll(student, subject(lecturer, 3, 30)).andExpect(status().isConflict());
        // 18 + 2 = 20: exactly at the limit is allowed
        enroll(student, subject(lecturer, 2, 30)).andExpect(status().isCreated());
    }

    @Test
    void studentDropsSubject() throws Exception {
        Subject subject = subject(lecturer(), 3, 30);
        User student = user(Role.STUDENT);
        enroll(student, subject).andExpect(status().isCreated());

        mockMvc.perform(delete("/api/enrollments/" + subject.getId()).header("Authorization", bearer(student)))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/enrollments/" + subject.getId()).header("Authorization", bearer(student)))
                .andExpect(status().isNotFound());
    }

    @Test
    void enrollingInUnknownSubjectIs404() throws Exception {
        mockMvc.perform(post("/api/enrollments")
                        .header("Authorization", bearer(user(Role.STUDENT)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subjectId\": 999999}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void lecturerCannotEnroll() throws Exception {
        enroll(lecturer(), subject(lecturer(), 3, 30)).andExpect(status().isForbidden());
    }

    @Test
    void onlyOwnerSeesClassList() throws Exception {
        User owner = lecturer();
        Subject subject = subject(owner, 3, 30);
        User student = user(Role.STUDENT);
        enroll(student, subject).andExpect(status().isCreated());

        mockMvc.perform(get("/api/subjects/" + subject.getId() + "/students").header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email").value(student.getEmail()));
        mockMvc.perform(get("/api/subjects/" + subject.getId() + "/students").header("Authorization", bearer(lecturer())))
                .andExpect(status().isForbidden());
    }

    @Test
    void lecturerCannotShrinkCapacityBelowEnrolledOrChangeCreditHours() throws Exception {
        User owner = lecturer();
        Subject subject = subject(owner, 3, 30);
        enroll(user(Role.STUDENT), subject).andExpect(status().isCreated());
        enroll(user(Role.STUDENT), subject).andExpect(status().isCreated());

        updateSubject(owner, subject, 3, 1).andExpect(status().isConflict());
        updateSubject(owner, subject, 4, 30).andExpect(status().isConflict());
        updateSubject(owner, subject, 3, 2).andExpect(status().isOk());
    }

    @Test
    void concurrentEnrollmentsNeverExceedCapacity() throws Exception {
        Subject subject = subject(lecturer(), 3, 1);
        int students = 8;
        List<Long> studentIds = new ArrayList<>();
        for (int i = 0; i < students; i++) {
            studentIds.add(user(Role.STUDENT).getId());
        }

        // Every thread waits at the latch, then all call enroll() at the same moment
        CountDownLatch startingGun = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger rejectedAsFull = new AtomicInteger();
        List<Future<?>> results = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(students)) {
            for (Long studentId : studentIds) {
                results.add(pool.submit(() -> {
                    startingGun.await();
                    try {
                        enrollmentService.enroll(subject.getId(), studentId);
                        succeeded.incrementAndGet();
                    } catch (ConflictException e) {
                        rejectedAsFull.incrementAndGet();
                    }
                    return null;
                }));
            }
            startingGun.countDown();
            for (Future<?> result : results) {
                result.get(); // rethrows anything unexpected, e.g. a deadlock
            }
        }

        assertThat(succeeded.get()).isEqualTo(1);
        assertThat(rejectedAsFull.get()).isEqualTo(students - 1);
        assertThat(enrollmentRepository.countBySubjectId(subject.getId())).isEqualTo(1);
    }

    private ResultActions enroll(User student, Subject subject) throws Exception {
        return mockMvc.perform(post("/api/enrollments")
                .header("Authorization", bearer(student))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"subjectId\": " + subject.getId() + "}"));
    }

    private ResultActions updateSubject(User owner, Subject subject, int creditHours, int capacity) throws Exception {
        return mockMvc.perform(put("/api/subjects/" + subject.getId())
                .header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"code": "%s", "name": "%s", "creditHours": %d, "capacity": %d}
                        """.formatted(subject.getCode(), subject.getName(), creditHours, capacity)));
    }

    private User lecturer() {
        return userRepository.save(new User("Dr Tan", uniqueEmail(), "unused-hash", Role.LECTURER));
    }

    private User user(Role role) {
        return userRepository.save(new User("Test User", uniqueEmail(), "unused-hash", role));
    }

    private Subject subject(User lecturer, int creditHours, int capacity) {
        String code = "ENR%03d".formatted(codeCounter.incrementAndGet());
        return subjectRepository.save(new Subject(code, "Subject " + code, creditHours, capacity, lecturer));
    }

    private String bearer(User user) {
        return "Bearer " + jwtService.createToken(user).accessToken();
    }

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@uni.edu";
    }
}
