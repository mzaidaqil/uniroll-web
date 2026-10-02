import { apiRequest } from './client'
import type { Enrollment, MyEnrollments } from './types'

export const myEnrollmentsKey = ['my-enrollments'] as const

export const getMyEnrollments = () => apiRequest<MyEnrollments>('/api/enrollments/me')

export const enroll = (subjectId: number) =>
  apiRequest<Enrollment>('/api/enrollments', { method: 'POST', body: { subjectId } })

export const drop = (subjectId: number) =>
  apiRequest<void>(`/api/enrollments/${subjectId}`, { method: 'DELETE' })
