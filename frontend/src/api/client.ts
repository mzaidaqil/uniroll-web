// Empty in development (the Vite proxy forwards /api); set VITE_API_URL when the API lives on another domain
const API_BASE_URL: string = import.meta.env.VITE_API_URL ?? ''

const TOKEN_KEY = 'uniroll.token'

// The JWT is kept in localStorage so a page refresh keeps you logged in
export const tokenStorage = {
  get: (): string | null => localStorage.getItem(TOKEN_KEY),
  set: (token: string) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
}

// An error response from the API, built from its ProblemDetail body
export class ApiError extends Error {
  readonly status: number
  readonly fieldErrors: Record<string, string>

  constructor(status: number, message: string, fieldErrors: Record<string, string> = {}) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

// AuthProvider registers its logout here, so an expired token anywhere logs the user out
let onUnauthorized: () => void = () => {}

export function setUnauthorizedHandler(handler: () => void) {
  onUnauthorized = handler
}

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  body?: unknown
}

export async function apiRequest<T>(path: string, { method = 'GET', body }: RequestOptions = {}): Promise<T> {
  const token = tokenStorage.get()
  const headers: Record<string, string> = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (token) headers['Authorization'] = `Bearer ${token}`

  const response = await fetch(`${API_BASE_URL}${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  })

  if (response.status === 401 && token) {
    onUnauthorized() // we sent a token and it was rejected: expired or invalid
  }
  if (!response.ok) {
    throw await toApiError(response)
  }
  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}

async function toApiError(response: Response): Promise<ApiError> {
  try {
    const problem = (await response.json()) as { title?: string; detail?: string; errors?: Record<string, string> }
    return new ApiError(response.status, problem.detail ?? problem.title ?? response.statusText, problem.errors)
  } catch {
    // Some errors (e.g. 401/403 from Spring Security) have no JSON body
    return new ApiError(response.status, response.statusText || `Request failed (${response.status})`)
  }
}
