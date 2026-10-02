import type { ReactNode } from 'react'
import { card } from './ui'

// Centered card used by the login and register pages
export function AuthCard({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div className="flex min-h-screen items-center justify-center px-4">
      <div className="w-full max-w-sm">
        <p className="mb-2 text-center text-2xl font-bold text-indigo-600">UniRoll</p>
        <h1 className="mb-6 text-center text-lg font-semibold text-slate-700">{title}</h1>
        <div className={card}>{children}</div>
      </div>
    </div>
  )
}
