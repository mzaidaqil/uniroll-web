package org.zayed.unirollweb.enrollment;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.zayed.unirollweb.common.CurrentUser;

// STUDENT only (SecurityConfig). Every action applies to the logged-in student, never to an id from the request.
@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EnrollmentResponse enroll(@Valid @RequestBody EnrollmentRequest request, @AuthenticationPrincipal Jwt jwt) {
        return enrollmentService.enroll(request.subjectId(), CurrentUser.id(jwt));
    }

    @DeleteMapping("/{subjectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void drop(@PathVariable Long subjectId, @AuthenticationPrincipal Jwt jwt) {
        enrollmentService.drop(subjectId, CurrentUser.id(jwt));
    }

    @GetMapping("/me")
    public MyEnrollmentsResponse myEnrollments(@AuthenticationPrincipal Jwt jwt) {
        return enrollmentService.getMyEnrollments(CurrentUser.id(jwt));
    }
}
