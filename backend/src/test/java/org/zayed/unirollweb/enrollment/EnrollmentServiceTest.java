package org.zayed.unirollweb.enrollment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.zayed.unirollweb.common.ConflictException;
import org.zayed.unirollweb.common.ForbiddenException;
import org.zayed.unirollweb.common.ResourceNotFoundException;
import org.zayed.unirollweb.subject.Subject;
import org.zayed.unirollweb.subject.SubjectRepository;
import org.zayed.unirollweb.user.Role;
import org.zayed.unirollweb.user.User;
import org.zayed.unirollweb.user.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    private static final Long LECTURER_ID = 1L;
    private static final Long STUDENT_ID = 2L;
    private static final Long SUBJECT_ID = 10L;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private UserRepository userRepository;

    private EnrollmentService enrollmentService;
    private User lecturer;
    private User student;
    private Subject subject;

    @BeforeEach
    void setUp() {
        enrollmentService = new EnrollmentService(enrollmentRepository, subjectRepository, userRepository);
        lecturer = withId(new User("Dr Tan", "tan@uni.edu", "hash", Role.LECTURER), LECTURER_ID);
        student = withId(new User("Aisyah", "aisyah@uni.edu", "hash", Role.STUDENT), STUDENT_ID);
        subject = withId(new Subject("CS101", "Programming", 3, 30, lecturer), SUBJECT_ID);
    }

    @Test
    void enrollLocksStudentThenSubjectAndSaves() {
        givenLockedRows();
        givenEnrollmentState(false, 0, 0);
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EnrollmentResponse response = enrollmentService.enroll(SUBJECT_ID, STUDENT_ID);

        assertThat(response.subjectCode()).isEqualTo("CS101");
        InOrder lockOrder = inOrder(userRepository, subjectRepository);
        lockOrder.verify(userRepository).findByIdForUpdate(STUDENT_ID);
        lockOrder.verify(subjectRepository).findByIdForUpdate(SUBJECT_ID);
    }

    @Test
    void enrollRejectsUnknownSubject() {
        when(userRepository.findByIdForUpdate(STUDENT_ID)).thenReturn(Optional.of(student));
        when(subjectRepository.findByIdForUpdate(SUBJECT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enrollmentService.enroll(SUBJECT_ID, STUDENT_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void enrollRejectsStudentAlreadyEnrolled() {
        givenLockedRows();
        when(enrollmentRepository.existsByStudentIdAndSubjectId(STUDENT_ID, SUBJECT_ID)).thenReturn(true);

        assertThatThrownBy(() -> enrollmentService.enroll(SUBJECT_ID, STUDENT_ID))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Already enrolled");
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void enrollRejectsFullSubject() {
        givenLockedRows();
        when(enrollmentRepository.existsByStudentIdAndSubjectId(STUDENT_ID, SUBJECT_ID)).thenReturn(false);
        when(enrollmentRepository.countBySubjectId(SUBJECT_ID)).thenReturn(30L);

        assertThatThrownBy(() -> enrollmentService.enroll(SUBJECT_ID, STUDENT_ID))
                .isInstanceOf(ConflictException.class)
                .hasMessage("CS101 is full");
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void enrollRejectsGoingOverCreditHourLimit() {
        givenLockedRows();
        givenEnrollmentState(false, 0, 18);

        assertThatThrownBy(() -> enrollmentService.enroll(SUBJECT_ID, STUDENT_ID))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("21 credit hours");
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void enrollAllowsReachingExactlyTheLimit() {
        givenLockedRows();
        givenEnrollmentState(false, 0, 17);
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        enrollmentService.enroll(SUBJECT_ID, STUDENT_ID);

        verify(enrollmentRepository).save(any(Enrollment.class));
    }

    @Test
    void dropRejectsWhenNotEnrolled() {
        when(enrollmentRepository.findByStudentIdAndSubjectId(STUDENT_ID, SUBJECT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enrollmentService.drop(SUBJECT_ID, STUDENT_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void dropDeletesEnrollment() {
        Enrollment enrollment = new Enrollment(student, subject);
        when(enrollmentRepository.findByStudentIdAndSubjectId(STUDENT_ID, SUBJECT_ID)).thenReturn(Optional.of(enrollment));

        enrollmentService.drop(SUBJECT_ID, STUDENT_ID);

        verify(enrollmentRepository).delete(enrollment);
    }

    @Test
    void myEnrollmentsAddsUpCreditHours() {
        Subject databases = withId(new Subject("CS201", "Databases", 4, 30, lecturer), 11L);
        when(enrollmentRepository.findByStudentIdOrderBySubjectCodeAsc(STUDENT_ID))
                .thenReturn(List.of(new Enrollment(student, subject), new Enrollment(student, databases)));

        MyEnrollmentsResponse response = enrollmentService.getMyEnrollments(STUDENT_ID);

        assertThat(response.totalCreditHours()).isEqualTo(7);
        assertThat(response.maxCreditHours()).isEqualTo(20);
        assertThat(response.enrollments()).extracting(EnrollmentResponse::subjectCode).containsExactly("CS101", "CS201");
    }

    @Test
    void classListIsOnlyForOwningLecturer() {
        when(subjectRepository.findById(SUBJECT_ID)).thenReturn(Optional.of(subject));

        assertThatThrownBy(() -> enrollmentService.getStudents(SUBJECT_ID, 99L))
                .isInstanceOf(ForbiddenException.class);
        verify(enrollmentRepository, never()).findBySubjectIdOrderByEnrolledAtAsc(any());
    }

    private void givenLockedRows() {
        when(userRepository.findByIdForUpdate(STUDENT_ID)).thenReturn(Optional.of(student));
        when(subjectRepository.findByIdForUpdate(SUBJECT_ID)).thenReturn(Optional.of(subject));
    }

    private void givenEnrollmentState(boolean alreadyEnrolled, long seatsTaken, long creditHoursSoFar) {
        when(enrollmentRepository.existsByStudentIdAndSubjectId(STUDENT_ID, SUBJECT_ID)).thenReturn(alreadyEnrolled);
        when(enrollmentRepository.countBySubjectId(SUBJECT_ID)).thenReturn(seatsTaken);
        when(enrollmentRepository.sumCreditHoursByStudentId(STUDENT_ID)).thenReturn(creditHoursSoFar);
    }

    private static <T> T withId(T entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
}
