import { apiRequest } from './client'
import type { Page, Subject } from './types'

// Every subject query key starts with this, so one invalidateQueries call refreshes all of them
export const subjectsKey = ['subjects'] as const

export function searchSubjects(search: string, page: number, size: number) {
  const params = new URLSearchParams({ search, page: String(page), size: String(size) })
  return apiRequest<Page<Subject>>(`/api/subjects?${params}`)
}
