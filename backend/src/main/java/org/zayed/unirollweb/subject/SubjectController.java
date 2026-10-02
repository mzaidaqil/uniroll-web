package org.zayed.unirollweb.subject;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.zayed.unirollweb.common.CurrentUser;
import org.zayed.unirollweb.enrollment.EnrolledStudentResponse;
import org.zayed.unirollweb.enrollment.EnrollmentService;

import java.net.URI;
import java.util.List;

// Role rules (LECTURER for POST/PUT/DELETE) are in SecurityConfig; owner checks are in SubjectService
@RestController
@RequestMapping("/api/subjects")
public class SubjectController {

    private final SubjectService subjectService;
    private final EnrollmentService enrollmentService;

    public SubjectController(SubjectService subjectService, EnrollmentService enrollmentService) {
        this.subjectService = subjectService;
        this.enrollmentService = enrollmentService;
    }

    // GET /api/subjects?search=prog&page=0&size=20, sorted by code
    @GetMapping
    public PagedModel<SubjectResponse> search(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return new PagedModel<>(subjectService.search(search, page, size));
    }

    @GetMapping("/{id}")
    public SubjectResponse getSubject(@PathVariable Long id) {
        return subjectService.getSubject(id);
    }

    @PostMapping
    public ResponseEntity<SubjectResponse> createSubject(@Valid @RequestBody SubjectRequest request,
                                                         @AuthenticationPrincipal Jwt jwt) {
        SubjectResponse created = subjectService.createSubject(request, CurrentUser.id(jwt));
        return ResponseEntity.created(URI.create("/api/subjects/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public SubjectResponse updateSubject(@PathVariable Long id, @Valid @RequestBody SubjectRequest request,
                                         @AuthenticationPrincipal Jwt jwt) {
        return subjectService.updateSubject(id, request, CurrentUser.id(jwt));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSubject(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        subjectService.deleteSubject(id, CurrentUser.id(jwt));
    }

    // The owning lecturer's class list
    @GetMapping("/{id}/students")
    public List<EnrolledStudentResponse> getStudents(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return enrollmentService.getStudents(id, CurrentUser.id(jwt));
    }
}
