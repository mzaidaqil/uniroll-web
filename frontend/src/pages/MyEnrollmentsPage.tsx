import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router'
import { drop, getMyEnrollments, myEnrollmentsKey } from '../api/enrollments'
import { subjectsKey } from '../api/subjects'
import type { Enrollment } from '../api/types'
import { CreditSummary } from '../components/CreditSummary'
import { ErrorAlert } from '../components/ErrorAlert'
import { buttonDanger } from '../components/ui'

export function MyEnrollmentsPage() {
  const queryClient = useQueryClient()
  const myEnrollments = useQuery({ queryKey: myEnrollmentsKey, queryFn: getMyEnrollments })

  const dropMutation = useMutation({
    mutationFn: drop,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: myEnrollmentsKey })
      queryClient.invalidateQueries({ queryKey: subjectsKey }) // a seat just opened up
    },
  })

  function confirmDrop(enrollment: Enrollment) {
    if (window.confirm(`Drop ${enrollment.subjectCode} ${enrollment.subjectName}?`)) {
      dropMutation.mutate(enrollment.subjectId)
    }
  }

  if (myEnrollments.isPending) {
    return <p className="text-slate-500">Loading your enrollments…</p>
  }
  if (myEnrollments.isError) {
    return <ErrorAlert error={myEnrollments.error} />
  }

  const { enrollments, totalCreditHours, maxCreditHours } = myEnrollments.data

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold">My enrollments</h1>
          <p className="mt-1 text-sm text-slate-600">
            {enrollments.length} {enrollments.length === 1 ? 'subject' : 'subjects'} this semester.
          </p>
        </div>
        <div className="sm:w-64">
          <CreditSummary total={totalCreditHours} max={maxCreditHours} />
        </div>
      </div>

      <ErrorAlert error={dropMutation.error} />

      {enrollments.length === 0 ? (
        <div className="rounded-lg bg-white p-8 text-center ring-1 ring-slate-200">
          <p className="text-slate-500">You're not enrolled in any subjects yet.</p>
          <Link to="/subjects" className="mt-2 inline-block font-semibold text-indigo-600 hover:text-indigo-500">
            Browse subjects →
          </Link>
        </div>
      ) : (
        <div className="overflow-x-auto rounded-lg bg-white shadow-sm ring-1 ring-slate-200">
          <table className="min-w-full divide-y divide-slate-200 text-sm">
            <thead className="bg-slate-50 text-left text-slate-600">
              <tr>
                <th className="px-4 py-3 font-semibold">Code</th>
                <th className="px-4 py-3 font-semibold">Subject</th>
                <th className="px-4 py-3 font-semibold">Lecturer</th>
                <th className="px-4 py-3 text-center font-semibold">Credits</th>
                <th className="px-4 py-3 font-semibold">Enrolled on</th>
                <th className="px-4 py-3" />
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {enrollments.map((enrollment) => (
                <tr key={enrollment.subjectId}>
                  <td className="px-4 py-3 font-mono font-semibold text-indigo-700">{enrollment.subjectCode}</td>
                  <td className="px-4 py-3">{enrollment.subjectName}</td>
                  <td className="px-4 py-3 text-slate-600">{enrollment.lecturerName}</td>
                  <td className="px-4 py-3 text-center">{enrollment.creditHours}</td>
                  <td className="px-4 py-3 text-slate-600">{new Date(enrollment.enrolledAt).toLocaleDateString()}</td>
                  <td className="px-4 py-3 text-right">
                    <button type="button" className={buttonDanger} disabled={dropMutation.isPending}
                            onClick={() => confirmDrop(enrollment)}>
                      {dropMutation.isPending && dropMutation.variables === enrollment.subjectId ? 'Dropping…' : 'Drop'}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
