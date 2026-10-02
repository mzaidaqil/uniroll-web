import { apiRequest } from './client'
import type { EnrolledStudent, Page, Subject, SubjectInput } from './types'

// Every subject query key starts with this, so one invalidateQueries call refreshes all of them
export const subjectsKey = ['subjects'] as const

// mine=true: only the logged-in lecturer's subjects
export function searchSubjects(search: string, page: number, size: number, mine = false) {
  const params = new URLSearchParams({ search, page: String(page), size: String(size), mine: String(mine) })
  return apiRequest<Page<Subject>>(`/api/subjects?${params}`)
}

export const getSubject = (id: number) => apiRequest<Subject>(`/api/subjects/${id}`)

export const createSubject = (input: SubjectInput) =>
  apiRequest<Subject>('/api/subjects', { method: 'POST', body: input })

export const updateSubject = (id: number, input: SubjectInput) =>
  apiRequest<Subject>(`/api/subjects/${id}`, { method: 'PUT', body: input })

export const deleteSubject = (id: number) => apiRequest<void>(`/api/subjects/${id}`, { method: 'DELETE' })

export const getClassList = (id: number) => apiRequest<EnrolledStudent[]>(`/api/subjects/${id}/students`)
