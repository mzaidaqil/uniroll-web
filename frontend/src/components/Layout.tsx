import { NavLink, Outlet } from 'react-router'
import { useAuth } from '../auth/useAuth'
import { buttonSecondary } from './ui'

const linkClass = ({ isActive }: { isActive: boolean }) =>
  `rounded-md px-3 py-2 text-sm font-medium ${
    isActive ? 'bg-indigo-50 text-indigo-700' : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'
  }`

// The frame around every logged-in page: nav bar on top, the current page below (<Outlet />)
export function Layout() {
  const { user, logout } = useAuth()

  return (
    <div className="min-h-screen">
      <header className="border-b border-slate-200 bg-white">
        <nav className="mx-auto flex max-w-5xl items-center gap-2 px-4 py-3">
          <span className="mr-4 text-lg font-bold text-indigo-600">UniRoll</span>
          {user?.role === 'STUDENT' && (
            <>
              <NavLink to="/subjects" className={linkClass}>
                Subjects
              </NavLink>
              <NavLink to="/my-enrollments" className={linkClass}>
                My enrollments
              </NavLink>
            </>
          )}
          {user?.role === 'LECTURER' && (
            <NavLink to="/teaching" className={linkClass}>
              My subjects
            </NavLink>
          )}
          <div className="ml-auto flex items-center gap-3">
            <span className="hidden text-sm text-slate-600 sm:inline">
              {user?.name}
              <span className="ml-2 rounded-full bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-600">
                {user?.role === 'LECTURER' ? 'Lecturer' : 'Student'}
              </span>
            </span>
            <button type="button" onClick={logout} className={buttonSecondary}>
              Log out
            </button>
          </div>
        </nav>
      </header>
      <main className="mx-auto max-w-5xl px-4 py-8">
        <Outlet />
      </main>
    </div>
  )
}
