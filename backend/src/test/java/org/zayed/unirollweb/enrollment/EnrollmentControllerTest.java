package org.zayed.unirollweb.enrollment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.zayed.unirollweb.common.ConflictException;
import org.zayed.unirollweb.common.JwtConfig;
import org.zayed.unirollweb.common.SecurityConfig;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.zayed.unirollweb.common.TestTokens.lecturer;
import static org.zayed.unirollweb.common.TestTokens.student;

@WebMvcTest(EnrollmentController.class)
@Import({SecurityConfig.class, JwtConfig.class})
class EnrollmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EnrollmentService enrollmentService;

    @Test
    void lecturerCannotEnroll() throws Exception {
        mockMvc.perform(post("/api/enrollments").with(lecturer(1))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"subjectId\": 10}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(enrollmentService);
    }

    @Test
    void studentEnrollsAsThemselves() throws Exception {
        when(enrollmentService.enroll(10L, 2L)).thenReturn(sampleEnrollment());

        mockMvc.perform(post("/api/enrollments").with(student(2))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"subjectId\": 10}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subjectCode").value("CS101"));
        verify(enrollmentService).enroll(10L, 2L);
    }

    @Test
    void enrollRequiresSubjectId() throws Exception {
        mockMvc.perform(post("/api/enrollments").with(student(2))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.subjectId").exists());
        verifyNoInteractions(enrollmentService);
    }

    @Test
    void businessRuleViolationBecomes409WithReason() throws Exception {
        when(enrollmentService.enroll(10L, 2L)).thenThrow(new ConflictException("CS101 is full"));

        mockMvc.perform(post("/api/enrollments").with(student(2))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"subjectId\": 10}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("CS101 is full"));
    }

    @Test
    void dropReturns204() throws Exception {
        mockMvc.perform(delete("/api/enrollments/10").with(student(2)))
                .andExpect(status().isNoContent());
        verify(enrollmentService).drop(10L, 2L);
    }

    @Test
    void myEnrollmentsReturnsListAndTotals() throws Exception {
        when(enrollmentService.getMyEnrollments(2L))
                .thenReturn(new MyEnrollmentsResponse(3, 20, List.of(sampleEnrollment())));

        mockMvc.perform(get("/api/enrollments/me").with(student(2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCreditHours").value(3))
                .andExpect(jsonPath("$.maxCreditHours").value(20))
                .andExpect(jsonPath("$.enrollments[0].lecturerName").value("Dr Tan"));
    }

    private static EnrollmentResponse sampleEnrollment() {
        return new EnrollmentResponse(10L, "CS101", "Programming", 3, "Dr Tan", Instant.parse("2026-10-01T00:00:00Z"));
    }
}
