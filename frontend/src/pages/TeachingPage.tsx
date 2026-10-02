import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'react-router'
import { deleteSubject, searchSubjects, subjectsKey } from '../api/subjects'
import type { Subject } from '../api/types'
import { ErrorAlert } from '../components/ErrorAlert'
import { Pagination } from '../components/Pagination'
import { buttonDanger, buttonPrimary, buttonSecondary, inputClass } from '../components/ui'
import { useDebouncedValue } from '../hooks/useDebouncedValue'

const PAGE_SIZE = 10

export function TeachingPage() {
  const queryClient = useQueryClient()
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const debouncedSearch = useDebouncedValue(search.trim())

  const subjects = useQuery({
    queryKey: [...subjectsKey, 'mine', debouncedSearch, page],
    queryFn: () => searchSubjects(debouncedSearch, page, PAGE_SIZE, true),
    placeholderData: keepPreviousData,
  })

  const deleteMutation = useMutation({
    mutationFn: deleteSubject,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: subjectsKey }),
  })

  function confirmDelete(subject: Subject) {
    const warning = subject.enrolledCount > 0
      ? `\n\n${subject.enrolledCount} enrolled student(s) will be removed from it.`
      : ''
    if (window.confirm(`Delete ${subject.code} ${subject.name}?${warning}`)) {
      deleteMutation.mutate(subject.id)
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold">My subjects</h1>
          <p className="mt-1 text-sm text-slate-600">Subjects you teach. Only you can change them or see their students.</p>
        </div>
        <Link to="/teaching/new" className={buttonPrimary}>
          New subject
        </Link>
      </div>

      <input type="search" placeholder="Search your subjects" className={inputClass} value={search}
             onChange={(e) => {
               setSearch(e.target.value)
               setPage(0)
             }} />

      <ErrorAlert error={deleteMutation.error} />
      <ErrorAlert error={subjects.error} />

      {subjects.isPending && <p className="text-slate-500">Loading your subjects…</p>}

      {subjects.data && subjects.data.content.length === 0 && (
        <div className="rounded-lg bg-white p-8 text-center ring-1 ring-slate-200">
          <p className="text-slate-500">
            {debouncedSearch ? `None of your subjects match "${debouncedSearch}".` : "You haven't created any subjects yet."}
          </p>
          {!debouncedSearch && (
            <Link to="/teaching/new" className="mt-2 inline-block font-semibold text-indigo-600 hover:text-indigo-500">
              Create your first subject →
            </Link>
          )}
        </div>
      )}

      {subjects.data && subjects.data.content.length > 0 && (
        <div className={`overflow-x-auto rounded-lg bg-white shadow-sm ring-1 ring-slate-200 ${
          subjects.isPlaceholderData ? 'opacity-60' : ''
        }`}>
          <table className="min-w-full divide-y divide-slate-200 text-sm">
            <thead className="bg-slate-50 text-left text-slate-600">
              <tr>
                <th className="px-4 py-3 font-semibold">Code</th>
                <th className="px-4 py-3 font-semibold">Subject</th>
                <th className="px-4 py-3 text-center font-semibold">Credits</th>
                <th className="px-4 py-3 text-center font-semibold">Enrolled</th>
                <th className="px-4 py-3" />
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {subjects.data.content.map((subject) => (
                <tr key={subject.id}>
                  <td className="px-4 py-3 font-mono font-semibold text-indigo-700">{subject.code}</td>
                  <td className="px-4 py-3">{subject.name}</td>
                  <td className="px-4 py-3 text-center">{subject.creditHours}</td>
                  <td className="px-4 py-3 text-center text-slate-600">
                    {subject.enrolledCount} / {subject.capacity}
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex justify-end gap-2">
                      <Link to={`/teaching/${subject.id}/students`} className={buttonSecondary}>Students</Link>
                      <Link to={`/teaching/${subject.id}/edit`} className={buttonSecondary}>Edit</Link>
                      <button type="button" className={buttonDanger} disabled={deleteMutation.isPending}
                              onClick={() => confirmDelete(subject)}>
                        Delete
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {subjects.data && <Pagination page={subjects.data.page} onPageChange={setPage} />}
    </div>
  )
}
