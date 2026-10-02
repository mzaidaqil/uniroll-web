import { ApiError } from '../api/client'

// Shows an error's message, e.g. "CS101 is full" from a 409 ProblemDetail
export function ErrorAlert({ error }: { error: unknown }) {
  if (!error) {
    return null
  }
  const message = error instanceof Error ? error.message : 'Something went wrong'
  const status = error instanceof ApiError ? error.status : undefined

  return (
    <div role="alert" className="rounded-md bg-red-50 p-3 text-sm text-red-700 ring-1 ring-red-200">
      {message}
      {status === undefined && ' (is the backend running?)'}
    </div>
  )
}
