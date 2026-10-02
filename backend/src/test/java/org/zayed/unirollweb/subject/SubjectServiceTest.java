package org.zayed.unirollweb.subject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.zayed.unirollweb.common.ConflictException;
import org.zayed.unirollweb.common.ForbiddenException;
import org.zayed.unirollweb.common.ResourceNotFoundException;
import org.zayed.unirollweb.enrollment.EnrollmentRepository;
import org.zayed.unirollweb.enrollment.EnrollmentRepository.SubjectEnrollmentCount;
import org.zayed.unirollweb.user.Role;
import org.zayed.unirollweb.user.User;
import org.zayed.unirollweb.user.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubjectServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_LECTURER_ID = 2L;
    private static final Long SUBJECT_ID = 10L;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private UserRepository userRepository;

    private SubjectService subjectService;
    private User owner;
    private Subject subject;

    @BeforeEach
    void setUp() {
        subjectService = new SubjectService(subjectRepository, enrollmentRepository, userRepository);
        owner = withId(new User("Dr Tan", "tan@uni.edu", "hash", Role.LECTURER), OWNER_ID);
        subject = withId(new Subject("CS101", "Programming", 3, 30, owner), SUBJECT_ID);
    }

    @Test
    void createRejectsDuplicateCode() {
        when(subjectRepository.existsByCode("CS101")).thenReturn(true);

        assertThatThrownBy(() -> subjectService.createSubject(request(" cs101 ", 3, 30), OWNER_ID))
                .isInstanceOf(ConflictException.class);
        verify(subjectRepository, never()).save(any());
    }

    @Test
    void createSavesSubjectOwnedByLecturer() {
        when(subjectRepository.existsByCode("CS101")).thenReturn(false);
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(subjectRepository.save(any(Subject.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SubjectResponse response = subjectService.createSubject(request("cs101", 3, 30), OWNER_ID);

        assertThat(response.code()).isEqualTo("CS101");
        assertThat(response.enrolledCount()).isZero();
        assertThat(response.lecturer().id()).isEqualTo(OWNER_ID);
    }

    @Test
    void getThrowsNotFoundForUnknownSubject() {
        when(subjectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subjectService.getSubject(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void searchUsesZeroForSubjectsWithNoEnrollments() {
        Subject other = withId(new Subject("CS201", "Databases", 4, 30, owner), 11L);
        Page<Subject> page = new PageImpl<>(List.of(subject, other));
        when(subjectRepository.search(eq("prog"), any(Pageable.class))).thenReturn(page);
        when(enrollmentRepository.countBySubjectIds(List.of(SUBJECT_ID, 11L))).thenReturn(List.of(count(SUBJECT_ID, 5)));

        List<SubjectResponse> results = subjectService.search("  prog ", 0, 20).getContent();

        assertThat(results).extracting(SubjectResponse::enrolledCount).containsExactly(5L, 0L);
    }

    @Test
    void updateRejectsLecturerWhoDoesNotOwnSubject() {
        when(subjectRepository.findByIdForUpdate(SUBJECT_ID)).thenReturn(Optional.of(subject));

        assertThatThrownBy(() -> subjectService.updateSubject(SUBJECT_ID, request("CS101", 3, 30), OTHER_LECTURER_ID))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void updateRejectsCapacityBelowEnrolledCount() {
        givenLockedSubjectWithEnrolled(5);

        assertThatThrownBy(() -> subjectService.updateSubject(SUBJECT_ID, request("CS101", 3, 4), OWNER_ID))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("5 students");
    }

    @Test
    void updateRejectsCreditHourChangeWhileStudentsEnrolled() {
        givenLockedSubjectWithEnrolled(1);

        assertThatThrownBy(() -> subjectService.updateSubject(SUBJECT_ID, request("CS101", 4, 30), OWNER_ID))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Credit hours");
    }

    @Test
    void updateChangesFieldsWhenRulesPass() {
        givenLockedSubjectWithEnrolled(0);

        SubjectResponse response = subjectService.updateSubject(SUBJECT_ID, request("cs102", 4, 40), OWNER_ID);

        assertThat(response.code()).isEqualTo("CS102");
        assertThat(subject.getCreditHours()).isEqualTo(4);
        assertThat(subject.getCapacity()).isEqualTo(40);
    }

    @Test
    void deleteRejectsLecturerWhoDoesNotOwnSubject() {
        when(subjectRepository.findById(SUBJECT_ID)).thenReturn(Optional.of(subject));

        assertThatThrownBy(() -> subjectService.deleteSubject(SUBJECT_ID, OTHER_LECTURER_ID))
                .isInstanceOf(ForbiddenException.class);
        verify(subjectRepository, never()).delete(any());
    }

    @Test
    void deleteRemovesOwnSubject() {
        when(subjectRepository.findById(SUBJECT_ID)).thenReturn(Optional.of(subject));

        subjectService.deleteSubject(SUBJECT_ID, OWNER_ID);

        verify(subjectRepository).delete(subject);
    }

    private void givenLockedSubjectWithEnrolled(long enrolled) {
        when(subjectRepository.findByIdForUpdate(SUBJECT_ID)).thenReturn(Optional.of(subject));
        when(subjectRepository.existsByCodeAndIdNot(any(), eq(SUBJECT_ID))).thenReturn(false);
        when(enrollmentRepository.countBySubjectId(SUBJECT_ID)).thenReturn(enrolled);
    }

    private static SubjectRequest request(String code, int creditHours, int capacity) {
        return new SubjectRequest(code, "Programming", creditHours, capacity);
    }

    private static SubjectEnrollmentCount count(Long subjectId, long enrolled) {
        return new SubjectEnrollmentCount() {
            @Override
            public Long getSubjectId() {
                return subjectId;
            }

            @Override
            public long getEnrolled() {
                return enrolled;
            }
        };
    }

    // Entities get their id from the database; in a unit test we set it directly
    private static <T> T withId(T entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
}
