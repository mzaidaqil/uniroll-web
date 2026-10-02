import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { enroll, getMyEnrollments, myEnrollmentsKey } from '../api/enrollments'
import { searchSubjects, subjectsKey } from '../api/subjects'
import type { Subject } from '../api/types'
import { CreditSummary } from '../components/CreditSummary'
import { ErrorAlert } from '../components/ErrorAlert'
import { Pagination } from '../components/Pagination'
import { buttonPrimary, inputClass } from '../components/ui'
import { useDebouncedValue } from '../hooks/useDebouncedValue'

const PAGE_SIZE = 10

export function SubjectsPage() {
  const queryClient = useQueryClient()
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const debouncedSearch = useDebouncedValue(search.trim())

  // The key includes search and page, so each combination is fetched and cached separately.
  // keepPreviousData keeps the old page on screen while the next one loads (no flicker).
  const subjects = useQuery({
    queryKey: [...subjectsKey, debouncedSearch, page],
    queryFn: () => searchSubjects(debouncedSearch, page, PAGE_SIZE),
    placeholderData: keepPreviousData,
  })
  const myEnrollments = useQuery({ queryKey: myEnrollmentsKey, queryFn: getMyEnrollments })

  const enrollMutation = useMutation({
    mutationFn: enroll,
    onSuccess: () => {
      // Seat counts and my credit hours changed: mark both stale so React Query refetches them
      queryClient.invalidateQueries({ queryKey: subjectsKey })
      queryClient.invalidateQueries({ queryKey: myEnrollmentsKey })
    },
  })

  const enrolledIds = new Set(myEnrollments.data?.enrollments.map((enrollment) => enrollment.subjectId))
  const totalHours = myEnrollments.data?.totalCreditHours ?? 0
  const maxHours = myEnrollments.data?.maxCreditHours ?? 20

  function enrollAction(subject: Subject) {
    if (enrolledIds.has(subject.id)) {
      return <span className="text-sm font-semibold text-green-700">Enrolled ✓</span>
    }
    if (subject.enrolledCount >= subject.capacity) {
      return <span className="text-sm font-medium text-slate-400">Full</span>
    }
    if (totalHours + subject.creditHours > maxHours) {
      return <span className="text-sm font-medium text-slate-400" title={`Would exceed ${maxHours} credit hours`}>
        Over {maxHours} h
      </span>
    }
    const isThisOnePending = enrollMutation.isPending && enrollMutation.variables === subject.id
    return (
      <button type="button" className={buttonPrimary} disabled={enrollMutation.isPending}
              onClick={() => enrollMutation.mutate(subject.id)}>
        {isThisOnePending ? 'Enrolling…' : 'Enroll'}
      </button>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold">Subjects</h1>
          <p className="mt-1 text-sm text-slate-600">Search by code or name and enroll in up to {maxHours} credit hours.</p>
        </div>
        {myEnrollments.data && (
          <div className="sm:w-64">
            <CreditSummary total={totalHours} max={maxHours} />
          </div>
        )}
      </div>

      <input type="search" placeholder="Search subjects, e.g. CS101 or programming" className={inputClass}
             value={search}
             onChange={(e) => {
               setSearch(e.target.value)
               setPage(0) // a new search starts from the first page
             }} />

      <ErrorAlert error={enrollMutation.error} />
      <ErrorAlert error={subjects.error} />

      {subjects.isPending && <p className="text-slate-500">Loading subjects…</p>}

      {subjects.data && subjects.data.content.length === 0 && (
        <p className="rounded-lg bg-white p-8 text-center text-slate-500 ring-1 ring-slate-200">
          {debouncedSearch ? `No subjects match "${debouncedSearch}".` : 'No subjects yet.'}
        </p>
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
                <th className="px-4 py-3 font-semibold">Lecturer</th>
                <th className="px-4 py-3 text-center font-semibold">Credits</th>
                <th className="px-4 py-3 text-center font-semibold">Seats</th>
                <th className="px-4 py-3" />
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {subjects.data.content.map((subject) => (
                <tr key={subject.id}>
                  <td className="px-4 py-3 font-mono font-semibold text-indigo-700">{subject.code}</td>
                  <td className="px-4 py-3">{subject.name}</td>
                  <td className="px-4 py-3 text-slate-600">{subject.lecturer.name}</td>
                  <td className="px-4 py-3 text-center">{subject.creditHours}</td>
                  <td className="px-4 py-3 text-center text-slate-600">
                    {subject.enrolledCount} / {subject.capacity}
                  </td>
                  <td className="px-4 py-3 text-right">{enrollAction(subject)}</td>
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
