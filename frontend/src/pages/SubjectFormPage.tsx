import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { ApiError } from '../api/client'
import { createSubject, getSubject, subjectsKey, updateSubject } from '../api/subjects'
import type { Subject, SubjectInput } from '../api/types'
import { ErrorAlert } from '../components/ErrorAlert'
import { FormField } from '../components/FormField'
import { buttonPrimary, buttonSecondary, card, inputClass } from '../components/ui'

// /teaching/new → empty form; /teaching/:id/edit → load the subject first, then show it in the form
export function SubjectFormPage() {
  const { id } = useParams()
  const subjectId = id === undefined ? null : Number(id)

  const existing = useQuery({
    queryKey: [...subjectsKey, 'detail', subjectId],
    queryFn: () => getSubject(subjectId!),
    enabled: subjectId !== null,
  })

  if (subjectId === null) {
    return <SubjectForm />
  }
  if (existing.isPending) {
    return <p className="text-slate-500">Loading subject…</p>
  }
  if (existing.isError) {
    return <ErrorAlert error={existing.error} />
  }
  // key: a different subject gets a fresh form instead of reusing the old one's typed values
  return <SubjectForm key={subjectId} subject={existing.data} />
}

interface FormValues {
  code: string
  name: string
  creditHours: string
  capacity: string
}

function SubjectForm({ subject }: { subject?: Subject }) {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const isEdit = subject !== undefined
  const hasStudents = (subject?.enrolledCount ?? 0) > 0

  // Inputs give us strings; they become numbers only when we send the request
  const [form, setForm] = useState<FormValues>({
    code: subject?.code ?? '',
    name: subject?.name ?? '',
    creditHours: String(subject?.creditHours ?? 3),
    capacity: String(subject?.capacity ?? 30),
  })

  const saveMutation = useMutation({
    mutationFn: (input: SubjectInput) => (isEdit ? updateSubject(subject.id, input) : createSubject(input)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: subjectsKey })
      navigate('/teaching')
    },
  })

  const error = saveMutation.error
  const fieldErrors = error instanceof ApiError ? error.fieldErrors : {}
  const generalError = Object.keys(fieldErrors).length === 0 ? error : null

  function update(field: keyof FormValues, value: string) {
    setForm((current) => ({ ...current, [field]: value }))
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    saveMutation.mutate({
      code: form.code,
      name: form.name,
      creditHours: Number(form.creditHours),
      capacity: Number(form.capacity),
    })
  }

  return (
    <div className="mx-auto max-w-xl space-y-6">
      <div>
        <Link to="/teaching" className="text-sm font-medium text-indigo-600 hover:text-indigo-500">← My subjects</Link>
        <h1 className="mt-2 text-2xl font-bold">{isEdit ? `Edit ${subject.code}` : 'New subject'}</h1>
      </div>

      <form onSubmit={handleSubmit} className={`${card} space-y-4`}>
        <ErrorAlert error={generalError} />
        <FormField label="Code" name="code" placeholder="CS101" required maxLength={10} error={fieldErrors.code}
                   value={form.code} onChange={(e) => update('code', e.target.value)} />
        <FormField label="Name" name="name" placeholder="Introduction to Programming" required maxLength={150}
                   error={fieldErrors.name} value={form.name} onChange={(e) => update('name', e.target.value)} />

        <div className="grid grid-cols-2 gap-4">
          <div>
            <label htmlFor="creditHours" className="block text-sm font-medium text-slate-700">Credit hours</label>
            <select id="creditHours" name="creditHours" className={`${inputClass} mt-1 disabled:bg-slate-100`}
                    disabled={hasStudents} value={form.creditHours}
                    onChange={(e) => update('creditHours', e.target.value)}>
              {[1, 2, 3, 4, 5, 6].map((hours) => (
                <option key={hours} value={hours}>{hours}</option>
              ))}
            </select>
            {hasStudents && <p className="mt-1 text-xs text-slate-500">Locked while students are enrolled.</p>}
            {fieldErrors.creditHours && <p className="mt-1 text-sm text-red-600">{fieldErrors.creditHours}</p>}
          </div>
          <div>
            <FormField label="Capacity" name="capacity" type="number" required
                       min={Math.max(1, subject?.enrolledCount ?? 0)} max={1000} error={fieldErrors.capacity}
                       value={form.capacity} onChange={(e) => update('capacity', e.target.value)} />
            {subject && hasStudents && (
              <p className="mt-1 text-xs text-slate-500">At least {subject.enrolledCount} (already enrolled).</p>
            )}
          </div>
        </div>

        <div className="flex justify-end gap-2 pt-2">
          <Link to="/teaching" className={buttonSecondary}>Cancel</Link>
          <button type="submit" className={buttonPrimary} disabled={saveMutation.isPending}>
            {saveMutation.isPending ? 'Saving…' : isEdit ? 'Save changes' : 'Create subject'}
          </button>
        </div>
      </form>
    </div>
  )
}
