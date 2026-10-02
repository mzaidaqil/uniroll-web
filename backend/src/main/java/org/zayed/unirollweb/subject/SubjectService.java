package org.zayed.unirollweb.subject;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zayed.unirollweb.common.ConflictException;
import org.zayed.unirollweb.common.ForbiddenException;
import org.zayed.unirollweb.common.ResourceNotFoundException;
import org.zayed.unirollweb.enrollment.EnrollmentRepository;
import org.zayed.unirollweb.enrollment.EnrollmentRepository.SubjectEnrollmentCount;
import org.zayed.unirollweb.user.User;
import org.zayed.unirollweb.user.UserRepository;

import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    public SubjectService(SubjectRepository subjectRepository, EnrollmentRepository enrollmentRepository,
                          UserRepository userRepository) {
        this.subjectRepository = subjectRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
    }

    // lecturerId null = every subject; otherwise only that lecturer's subjects
    @Transactional(readOnly = true)
    public Page<SubjectResponse> search(String search, Long lecturerId, int page, int size) {
        String term = search.trim();
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("code"));
        Page<Subject> subjects = lecturerId == null
                ? subjectRepository.search(term, pageRequest)
                : subjectRepository.searchByLecturer(term, lecturerId, pageRequest);
        if (subjects.isEmpty()) {
            return subjects.map(subject -> SubjectResponse.from(subject, 0));
        }

        Map<Long, Long> enrolledCounts = enrollmentRepository
                .countBySubjectIds(subjects.map(Subject::getId).getContent())
                .stream()
                .collect(Collectors.toMap(SubjectEnrollmentCount::getSubjectId, SubjectEnrollmentCount::getEnrolled));
        return subjects.map(subject -> SubjectResponse.from(subject, enrolledCounts.getOrDefault(subject.getId(), 0L)));
    }

    @Transactional(readOnly = true)
    public SubjectResponse getSubject(Long id) {
        Subject subject = subjectRepository.findById(id).orElseThrow(() -> notFound(id));
        return SubjectResponse.from(subject, enrollmentRepository.countBySubjectId(id));
    }

    @Transactional
    public SubjectResponse createSubject(SubjectRequest request, Long lecturerId) {
        String code = Subject.normalizeCode(request.code());
        if (subjectRepository.existsByCode(code)) {
            throw duplicateCode(code);
        }
        User lecturer = userRepository.findById(lecturerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + lecturerId));

        Subject subject = new Subject(code, request.name().trim(), request.creditHours(), request.capacity(), lecturer);
        return SubjectResponse.from(subjectRepository.save(subject), 0);
    }

    @Transactional
    public SubjectResponse updateSubject(Long id, SubjectRequest request, Long lecturerId) {
        // Locked, so no student can enroll between the capacity check below and the commit
        Subject subject = subjectRepository.findByIdForUpdate(id).orElseThrow(() -> notFound(id));
        requireOwner(subject, lecturerId);

        String code = Subject.normalizeCode(request.code());
        if (subjectRepository.existsByCodeAndIdNot(code, id)) {
            throw duplicateCode(code);
        }

        long enrolled = enrollmentRepository.countBySubjectId(id);
        if (request.capacity() < enrolled) {
            throw new ConflictException("Capacity cannot be lower than the " + enrolled + " students already enrolled");
        }
        // Changing credit hours could push enrolled students over their 20-hour limit
        if (enrolled > 0 && request.creditHours() != subject.getCreditHours()) {
            throw new ConflictException("Credit hours cannot change while students are enrolled");
        }

        subject.update(code, request.name().trim(), request.creditHours(), request.capacity());
        return SubjectResponse.from(subject, enrolled);
    }

    @Transactional
    public void deleteSubject(Long id, Long lecturerId) {
        Subject subject = subjectRepository.findById(id).orElseThrow(() -> notFound(id));
        requireOwner(subject, lecturerId);
        // The database deletes this subject's enrollments too (ON DELETE CASCADE)
        subjectRepository.delete(subject);
    }

    private static void requireOwner(Subject subject, Long lecturerId) {
        if (!subject.isOwnedBy(lecturerId)) {
            throw new ForbiddenException("You can only change your own subjects");
        }
    }

    private static ResourceNotFoundException notFound(Long id) {
        return new ResourceNotFoundException("Subject not found: " + id);
    }

    private static ConflictException duplicateCode(String code) {
        return new ConflictException("Subject code already exists: " + code);
    }
}
