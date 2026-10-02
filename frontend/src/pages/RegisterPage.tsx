import { useMutation } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router'
import { login, register } from '../api/auth'
import { ApiError } from '../api/client'
import type { RegisterInput, Role } from '../api/types'
import { useAuth } from '../auth/useAuth'
import { AuthCard } from '../components/AuthCard'
import { ErrorAlert } from '../components/ErrorAlert'
import { FormField } from '../components/FormField'
import { buttonPrimary } from '../components/ui'

const roles: { value: Role; label: string }[] = [
  { value: 'STUDENT', label: 'Student' },
  { value: 'LECTURER', label: 'Lecturer' },
]

export function RegisterPage() {
  const auth = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState<RegisterInput>({ name: '', email: '', password: '', role: 'STUDENT' })

  // Register, then log straight in with the same details so the user doesn't have to type them again
  const registerMutation = useMutation({
    mutationFn: async (input: RegisterInput) => {
      await register(input)
      return login({ email: input.email, password: input.password })
    },
    onSuccess: (token) => {
      auth.login(token.accessToken)
      navigate('/', { replace: true })
    },
  })

  if (auth.user) {
    return <Navigate to="/" replace />
  }

  const error = registerMutation.error
  const fieldErrors = error instanceof ApiError ? error.fieldErrors : {}
  // Field problems are shown under each input; anything else (e.g. "email already registered") goes on top
  const generalError = Object.keys(fieldErrors).length === 0 ? error : null

  function update(field: keyof RegisterInput, value: string) {
    setForm((current) => ({ ...current, [field]: value }))
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    registerMutation.mutate(form)
  }

  return (
    <AuthCard title="Create an account">
      <form onSubmit={handleSubmit} className="space-y-4">
        <ErrorAlert error={generalError} />
        <fieldset>
          <legend className="block text-sm font-medium text-slate-700">I am a</legend>
          <div className="mt-1 grid grid-cols-2 gap-2">
            {roles.map((role) => (
              <label key={role.value}
                     className={`cursor-pointer rounded-md px-3 py-2 text-center text-sm font-semibold ring-1 ${
                       form.role === role.value
                         ? 'bg-indigo-600 text-white ring-indigo-600'
                         : 'bg-white text-slate-700 ring-slate-300 hover:bg-slate-50'
                     }`}>
                <input type="radio" name="role" value={role.value} className="sr-only"
                       checked={form.role === role.value} onChange={() => update('role', role.value)} />
                {role.label}
              </label>
            ))}
          </div>
        </fieldset>
        <FormField label="Full name" name="name" autoComplete="name" required error={fieldErrors.name}
                   value={form.name} onChange={(e) => update('name', e.target.value)} />
        <FormField label="Email" name="email" type="email" autoComplete="email" required error={fieldErrors.email}
                   value={form.email} onChange={(e) => update('email', e.target.value)} />
        <FormField label="Password" name="password" type="password" autoComplete="new-password" required
                   minLength={8} maxLength={72} error={fieldErrors.password}
                   value={form.password} onChange={(e) => update('password', e.target.value)} />
        <p className="text-xs text-slate-500">At least 8 characters.</p>
        <button type="submit" disabled={registerMutation.isPending} className={`${buttonPrimary} w-full`}>
          {registerMutation.isPending ? 'Creating account…' : 'Create account'}
        </button>
      </form>
      <p className="mt-4 text-center text-sm text-slate-600">
        Already registered?{' '}
        <Link to="/login" className="font-semibold text-indigo-600 hover:text-indigo-500">
          Log in
        </Link>
      </p>
    </AuthCard>
  )
}
