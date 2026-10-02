import { apiRequest } from './client'
import type { LoginInput, RegisterInput, TokenResponse, User } from './types'

export const register = (input: RegisterInput) =>
  apiRequest<User>('/api/auth/register', { method: 'POST', body: input })

export const login = (input: LoginInput) =>
  apiRequest<TokenResponse>('/api/auth/login', { method: 'POST', body: input })

export const getCurrentUser = () => apiRequest<User>('/api/users/me')
