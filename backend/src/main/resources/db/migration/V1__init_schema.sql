CREATE TABLE users (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE CHECK (email = LOWER(email)),
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL CHECK (role IN ('LECTURER', 'STUDENT')),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE subjects (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code         VARCHAR(10)  NOT NULL UNIQUE,
    name         VARCHAR(150) NOT NULL,
    credit_hours INTEGER      NOT NULL CHECK (credit_hours BETWEEN 1 AND 6),
    capacity     INTEGER      NOT NULL CHECK (capacity > 0),
    lecturer_id  BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Postgres does not index foreign keys automatically; "subjects taught by X" needs this
CREATE INDEX idx_subjects_lecturer_id ON subjects (lecturer_id);

CREATE TABLE enrollments (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    student_id  BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    subject_id  BIGINT      NOT NULL REFERENCES subjects (id) ON DELETE CASCADE,
    enrolled_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_enrollments_student_subject UNIQUE (student_id, subject_id)
);

-- The unique constraint above already covers lookups by student_id (its first column)
CREATE INDEX idx_enrollments_subject_id ON enrollments (subject_id);
