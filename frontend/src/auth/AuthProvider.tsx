import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { getCurrentUser } from '../api/auth'
import { setUnauthorizedHandler, tokenStorage } from '../api/client'
import { AuthContext } from './authContext'

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [token, setToken] = useState<string | null>(() => tokenStorage.get())

  const login = useCallback((newToken: string) => {
    tokenStorage.set(newToken)
    setToken(newToken)
  }, [])

  const logout = useCallback(() => {
    tokenStorage.clear()
    setToken(null)
    queryClient.clear() // forget the previous user's cached data
  }, [queryClient])

  useEffect(() => {
    setUnauthorizedHandler(logout)
  }, [logout])

  // The token only holds an id and role; the name and email come from /api/users/me.
  // The token is part of the key, so logging in as someone else fetches fresh data.
  const me = useQuery({
    queryKey: ['me', token],
    queryFn: getCurrentUser,
    enabled: token !== null,
    staleTime: Infinity,
  })

  const value = useMemo(
    () => ({
      user: token ? (me.data ?? null) : null,
      isLoading: token !== null && me.isPending,
      login,
      logout,
    }),
    [token, me.data, me.isPending, login, logout],
  )

  return <AuthContext value={value}>{children}</AuthContext>
}
