import { createContext } from 'react'
import type { User } from '../api/types'

export interface AuthState {
  user: User | null
  // True while a saved token exists but /api/users/me hasn't answered yet
  isLoading: boolean
  login: (token: string) => void
  logout: () => void
}

export const AuthContext = createContext<AuthState | null>(null)
