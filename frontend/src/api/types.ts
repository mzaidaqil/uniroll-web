// TypeScript versions of the backend's request/response records. Dates arrive as ISO strings.

export type Role = 'LECTURER' | 'STUDENT'

export interface User {
  id: number
  name: string
  email: string
  role: Role
  createdAt: string
}

export interface RegisterInput {
  name: string
  email: string
  password: string
  role: Role
}

export interface LoginInput {
  email: string
  password: string
}

export interface TokenResponse {
  accessToken: string
  tokenType: string
  expiresInSeconds: number
}

export interface Subject {
  id: number
  code: string
  name: string
  creditHours: number
  capacity: number
  enrolledCount: number
  lecturer: { id: number; name: string }
  createdAt: string
}

export interface SubjectInput {
  code: string
  name: string
  creditHours: number
  capacity: number
}

// Spring Data's PagedModel
export interface Page<T> {
  content: T[]
  page: { size: number; number: number; totalElements: number; totalPages: number }
}

export interface Enrollment {
  subjectId: number
  subjectCode: string
  subjectName: string
  creditHours: number
  lecturerName: string
  enrolledAt: string
}

export interface MyEnrollments {
  totalCreditHours: number
  maxCreditHours: number
  enrollments: Enrollment[]
}

export interface EnrolledStudent {
  studentId: number
  name: string
  email: string
  enrolledAt: string
}
