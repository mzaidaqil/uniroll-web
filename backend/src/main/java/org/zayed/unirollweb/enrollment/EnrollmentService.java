package org.zayed.unirollweb.enrollment;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zayed.unirollweb.common.ConflictException;
import org.zayed.unirollweb.common.ForbiddenException;
import org.zayed.unirollweb.common.ResourceNotFoundException;
import org.zayed.unirollweb.subject.Subject;
import org.zayed.unirollweb.subject.SubjectRepository;
import org.zayed.unirollweb.user.User;
import org.zayed.unirollweb.user.UserRepository;

import java.util.List;

@Service
public class EnrollmentService {

    static final int MAX_CREDIT_HOURS = 20;

    private final EnrollmentRepository enrollmentRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;

    public EnrollmentService(EnrollmentRepository enrollmentRepository, SubjectRepository subjectRepository,
                             UserRepository userRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.subjectRepository = subjectRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public EnrollmentResponse enroll(Long subjectId, Long studentId) {
        // Two row locks, always in the same order (student, then subject) so two requests can't deadlock:
        // - the student lock makes this student's enrollments run one at a time (credit-hour cap)
        // - the subject lock makes enrollments in this subject run one at a time (capacity)
        User student = userRepository.findByIdForUpdate(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + studentId));
        Subject subject = subjectRepository.findByIdForUpdate(subjectId)
                .orElseThrow(() -> subjectNotFound(subjectId));

        if (enrollmentRepository.existsByStudentIdAndSubjectId(studentId, subjectId)) {
            throw new ConflictException("Already enrolled in " + subject.getCode());
        }
        if (enrollmentRepository.countBySubjectId(subjectId) >= subject.getCapacity()) {
            throw new ConflictException(subject.getCode() + " is full");
        }
        long currentHours = enrollmentRepository.sumCreditHoursByStudentId(studentId);
        if (currentHours + subject.getCreditHours() > MAX_CREDIT_HOURS) {
            throw new ConflictException("Enrolling in " + subject.getCode() + " would bring you to "
                    + (currentHours + subject.getCreditHours()) + " credit hours; the limit is " + MAX_CREDIT_HOURS);
        }

        return EnrollmentResponse.from(enrollmentRepository.save(new Enrollment(student, subject)));
    }

    @Transactional
    public void drop(Long subjectId, Long studentId) {
        Enrollment enrollment = enrollmentRepository.findByStudentIdAndSubjectId(studentId, subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Not enrolled in subject " + subjectId));
        enrollmentRepository.delete(enrollment);
    }

    @Transactional(readOnly = true)
    public MyEnrollmentsResponse getMyEnrollments(Long studentId) {
        List<EnrollmentResponse> enrollments = enrollmentRepository.findByStudentIdOrderBySubjectCodeAsc(studentId)
                .stream()
                .map(EnrollmentResponse::from)
                .toList();
        int totalCreditHours = enrollments.stream().mapToInt(EnrollmentResponse::creditHours).sum();
        return new MyEnrollmentsResponse(totalCreditHours, MAX_CREDIT_HOURS, enrollments);
    }

    @Transactional(readOnly = true)
    public List<EnrolledStudentResponse> getStudents(Long subjectId, Long lecturerId) {
        Subject subject = subjectRepository.findById(subjectId).orElseThrow(() -> subjectNotFound(subjectId));
        if (!subject.isOwnedBy(lecturerId)) {
            throw new ForbiddenException("You can only view students of your own subjects");
        }
        return enrollmentRepository.findBySubjectIdOrderByEnrolledAtAsc(subjectId)
                .stream()
                .map(EnrolledStudentResponse::from)
                .toList();
    }

    private static ResourceNotFoundException subjectNotFound(Long id) {
        return new ResourceNotFoundException("Subject not found: " + id);
    }
}
