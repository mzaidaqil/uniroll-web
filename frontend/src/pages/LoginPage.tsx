import { useMutation } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router'
import { login } from '../api/auth'
import { useAuth } from '../auth/useAuth'
import { AuthCard } from '../components/AuthCard'
import { ErrorAlert } from '../components/ErrorAlert'
import { FormField } from '../components/FormField'
import { buttonPrimary } from '../components/ui'

export function LoginPage() {
  const auth = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  // RequireAuth stores the page you were trying to open, so we can send you back after login
  const returnTo = (location.state as { from?: string } | null)?.from ?? '/'

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')

  const loginMutation = useMutation({
    mutationFn: login,
    onSuccess: (token) => {
      auth.login(token.accessToken)
      navigate(returnTo, { replace: true })
    },
  })

  if (auth.user) {
    return <Navigate to="/" replace />
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault() // stop the browser's full-page form submit
    loginMutation.mutate({ email, password })
  }

  return (
    <AuthCard title="Log in to your account">
      <form onSubmit={handleSubmit} className="space-y-4">
        <ErrorAlert error={loginMutation.error} />
        <FormField label="Email" name="email" type="email" autoComplete="email" required
                   value={email} onChange={(e) => setEmail(e.target.value)} />
        <FormField label="Password" name="password" type="password" autoComplete="current-password" required
                   value={password} onChange={(e) => setPassword(e.target.value)} />
        <button type="submit" disabled={loginMutation.isPending} className={`${buttonPrimary} w-full`}>
          {loginMutation.isPending ? 'Logging in…' : 'Log in'}
        </button>
      </form>
      <p className="mt-4 text-center text-sm text-slate-600">
        No account?{' '}
        <Link to="/register" className="font-semibold text-indigo-600 hover:text-indigo-500">
          Register
        </Link>
      </p>
    </AuthCard>
  )
}
