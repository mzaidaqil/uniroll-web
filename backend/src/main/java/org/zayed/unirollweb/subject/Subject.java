package org.zayed.unirollweb.subject;

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
import org.zayed.unirollweb.user.User;

import java.time.Instant;
import java.util.Locale;

@Entity
@Table(name = "subjects")
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "credit_hours", nullable = false)
    private int creditHours;

    @Column(nullable = false)
    private int capacity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lecturer_id", nullable = false)
    private User lecturer;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Subject() {
        // required by JPA
    }

    public Subject(String code, String name, int creditHours, int capacity, User lecturer) {
        this.code = normalizeCode(code);
        this.name = name;
        this.creditHours = creditHours;
        this.capacity = capacity;
        this.lecturer = lecturer;
    }

    // "cs101 " and "CS101" are the same subject
    public static String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    // Hibernate notices the changed fields and issues the UPDATE when the transaction commits
    public void update(String code, String name, int creditHours, int capacity) {
        this.code = normalizeCode(code);
        this.name = name;
        this.creditHours = creditHours;
        this.capacity = capacity;
    }

    public boolean isOwnedBy(Long userId) {
        return lecturer.getId().equals(userId);
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public int getCreditHours() {
        return creditHours;
    }

    public int getCapacity() {
        return capacity;
    }

    public User getLecturer() {
        return lecturer;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
