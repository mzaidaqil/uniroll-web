package org.zayed.unirollweb.enrollment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.zayed.unirollweb.subject.Subject;
import org.zayed.unirollweb.user.User;

import java.time.Instant;

@Entity
@Table(name = "enrollments")
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(name = "enrolled_at", nullable = false, updatable = false)
    private Instant enrolledAt;

    protected Enrollment() {
        // required by JPA
    }

    public Enrollment(User student, Subject subject) {
        this.student = student;
        this.subject = subject;
    }

    @PrePersist
    void onCreate() {
        enrolledAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public User getStudent() {
        return student;
    }

    public Subject getSubject() {
        return subject;
    }

    public Instant getEnrolledAt() {
        return enrolledAt;
    }
}
