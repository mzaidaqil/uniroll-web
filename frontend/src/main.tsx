import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router'
import { ApiError } from './api/client'
import App from './App.tsx'
import { AuthProvider } from './auth/AuthProvider'
import './index.css'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      // Retry network hiccups and 5xx errors once, but not 4xx: asking again won't change "not found" or "forbidden"
      retry: (failureCount, error) =>
        !(error instanceof ApiError && error.status < 500) && failureCount < 1,
    },
  },
})

// Providers wrap the app so every component can use them: React Query's cache, the router, the logged-in user
createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AuthProvider>
          <App />
        </AuthProvider>
      </BrowserRouter>
    </QueryClientProvider>
  </StrictMode>,
)
