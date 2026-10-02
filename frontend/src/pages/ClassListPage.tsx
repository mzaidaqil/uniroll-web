import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router'
import { getClassList, getSubject, subjectsKey } from '../api/subjects'
import { ErrorAlert } from '../components/ErrorAlert'

// /teaching/:id/students: the owning lecturer's list of enrolled students
export function ClassListPage() {
  const subjectId = Number(useParams().id)

  // Two independent requests; React Query runs them in parallel
  const subject = useQuery({ queryKey: [...subjectsKey, 'detail', subjectId], queryFn: () => getSubject(subjectId) })
  const students = useQuery({ queryKey: [...subjectsKey, 'students', subjectId], queryFn: () => getClassList(subjectId) })

  return (
    <div className="space-y-6">
      <div>
        <Link to="/teaching" className="text-sm font-medium text-indigo-600 hover:text-indigo-500">← My subjects</Link>
        {subject.data && (
          <>
            <h1 className="mt-2 text-2xl font-bold">
              <span className="font-mono text-indigo-700">{subject.data.code}</span> {subject.data.name}
            </h1>
            <p className="mt-1 text-sm text-slate-600">
              {subject.data.enrolledCount} of {subject.data.capacity} seats taken · {subject.data.creditHours} credit hours
            </p>
          </>
        )}
      </div>

      <ErrorAlert error={subject.error ?? students.error} />

      {students.isPending && <p className="text-slate-500">Loading students…</p>}

      {students.data && students.data.length === 0 && (
        <p className="rounded-lg bg-white p-8 text-center text-slate-500 ring-1 ring-slate-200">
          No students have enrolled yet.
        </p>
      )}

      {students.data && students.data.length > 0 && (
        <div className="overflow-x-auto rounded-lg bg-white shadow-sm ring-1 ring-slate-200">
          <table className="min-w-full divide-y divide-slate-200 text-sm">
            <thead className="bg-slate-50 text-left text-slate-600">
              <tr>
                <th className="px-4 py-3 font-semibold">#</th>
                <th className="px-4 py-3 font-semibold">Name</th>
                <th className="px-4 py-3 font-semibold">Email</th>
                <th className="px-4 py-3 font-semibold">Enrolled on</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {students.data.map((student, index) => (
                <tr key={student.studentId}>
                  <td className="px-4 py-3 text-slate-400">{index + 1}</td>
                  <td className="px-4 py-3 font-medium">{student.name}</td>
                  <td className="px-4 py-3 text-slate-600">{student.email}</td>
                  <td className="px-4 py-3 text-slate-600">{new Date(student.enrolledAt).toLocaleDateString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
