package org.zayed.unirollweb.subject;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.zayed.unirollweb.common.ForbiddenException;
import org.zayed.unirollweb.common.JwtConfig;
import org.zayed.unirollweb.common.ResourceNotFoundException;
import org.zayed.unirollweb.common.SecurityConfig;
import org.zayed.unirollweb.enrollment.EnrollmentService;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.zayed.unirollweb.common.TestTokens.lecturer;
import static org.zayed.unirollweb.common.TestTokens.student;

/**
 * Web layer only: security rules, request validation, JSON shape and exception-to-status mapping.
 * Services are mocks, so no database is involved.
 */
@WebMvcTest(SubjectController.class)
@Import({SecurityConfig.class, JwtConfig.class})
class SubjectControllerTest {

    private static final String VALID_BODY = """
            {"code": "CS101", "name": "Programming", "creditHours": 3, "capacity": 30}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SubjectService subjectService;

    @MockitoBean
    private EnrollmentService enrollmentService;

    @Test
    void listRequiresToken() throws Exception {
        mockMvc.perform(get("/api/subjects"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(subjectService);
    }

    @Test
    void listPassesSearchAndPagingToService() throws Exception {
        when(subjectService.search("prog", null, 1, 5))
                .thenReturn(new PageImpl<>(List.of(sampleSubject()), PageRequest.of(1, 5), 6));

        mockMvc.perform(get("/api/subjects").param("search", "prog").param("page", "1").param("size", "5")
                        .with(student(2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("CS101"))
                .andExpect(jsonPath("$.content[0].lecturer.name").value("Dr Tan"))
                .andExpect(jsonPath("$.page.number").value(1))
                .andExpect(jsonPath("$.page.totalElements").value(6));
    }

    @Test
    void mineTakesLecturerIdFromToken() throws Exception {
        when(subjectService.search("", 1L, 0, 20)).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/subjects").param("mine", "true").with(lecturer(1)))
                .andExpect(status().isOk());
        verify(subjectService).search("", 1L, 0, 20);
    }

    @Test
    void listRejectsPageSizeAbove50() throws Exception {
        mockMvc.perform(get("/api/subjects").param("size", "51").with(student(2)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(subjectService);
    }

    @Test
    void studentCannotCreateSubject() throws Exception {
        mockMvc.perform(post("/api/subjects").with(student(2))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isForbidden());
        verifyNoInteractions(subjectService);
    }

    @Test
    void lecturerCreatesSubjectAsThemselves() throws Exception {
        when(subjectService.createSubject(any(SubjectRequest.class), eq(1L))).thenReturn(sampleSubject());

        mockMvc.perform(post("/api/subjects").with(lecturer(1))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/subjects/10"))
                .andExpect(jsonPath("$.id").value(10));
        // The lecturer id comes from the token, never from the request body
        verify(subjectService).createSubject(any(SubjectRequest.class), eq(1L));
    }

    @Test
    void createRejectsInvalidBodyWithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/subjects").with(lecturer(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "CS-101", "name": " ", "creditHours": 0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors.code").exists())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.creditHours").exists())
                .andExpect(jsonPath("$.errors.capacity").exists());
        verifyNoInteractions(subjectService);
    }

    @Test
    void ownershipFailureFromServiceBecomes403() throws Exception {
        when(subjectService.updateSubject(eq(10L), any(SubjectRequest.class), eq(1L)))
                .thenThrow(new ForbiddenException("You can only change your own subjects"));

        mockMvc.perform(put("/api/subjects/10").with(lecturer(1))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("You can only change your own subjects"));
    }

    @Test
    void unknownSubjectBecomes404ProblemDetail() throws Exception {
        when(subjectService.getSubject(99L)).thenThrow(new ResourceNotFoundException("Subject not found: 99"));

        mockMvc.perform(get("/api/subjects/99").with(student(2)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value("Subject not found: 99"));
    }

    @Test
    void studentCannotSeeClassList() throws Exception {
        mockMvc.perform(get("/api/subjects/10/students").with(student(2)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(enrollmentService);
    }

    private static SubjectResponse sampleSubject() {
        return new SubjectResponse(10L, "CS101", "Programming", 3, 30, 0,
                new SubjectResponse.LecturerSummary(1L, "Dr Tan"), Instant.parse("2026-10-01T00:00:00Z"));
    }
}
