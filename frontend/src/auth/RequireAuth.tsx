import { Navigate, Outlet, useLocation } from 'react-router'
import type { Role } from '../api/types'
import { useAuth } from './useAuth'

// Wraps a group of routes: not logged in → /login (remembering where you were going);
// logged in with the wrong role → home. This is for user experience only:
// the backend checks the token and role on every request anyway.
export function RequireAuth({ role }: { role?: Role }) {
  const { user, isLoading } = useAuth()
  const location = useLocation()

  if (isLoading) {
    return <p className="p-8 text-center text-slate-500">Loading…</p>
  }
  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }
  if (role && user.role !== role) {
    return <Navigate to="/" replace />
  }
  return <Outlet />
}
