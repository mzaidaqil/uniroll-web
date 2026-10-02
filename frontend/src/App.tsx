import { Navigate, Route, Routes } from 'react-router'
import { RequireAuth } from './auth/RequireAuth'
import { useAuth } from './auth/useAuth'
import { Layout } from './components/Layout'
import { ClassListPage } from './pages/ClassListPage'
import { LoginPage } from './pages/LoginPage'
import { MyEnrollmentsPage } from './pages/MyEnrollmentsPage'
import { RegisterPage } from './pages/RegisterPage'
import { SubjectFormPage } from './pages/SubjectFormPage'
import { SubjectsPage } from './pages/SubjectsPage'
import { TeachingPage } from './pages/TeachingPage'

// "/" sends each role to its own start page
function HomeRedirect() {
  const { user } = useAuth()
  return <Navigate to={user?.role === 'LECTURER' ? '/teaching' : '/subjects'} replace />
}

// Nested routes: RequireAuth guards everything inside it, Layout adds the nav bar,
// and the inner RequireAuth groups add the role check
export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />

      <Route element={<RequireAuth />}>
        <Route element={<Layout />}>
          <Route index element={<HomeRedirect />} />

          <Route element={<RequireAuth role="STUDENT" />}>
            <Route path="/subjects" element={<SubjectsPage />} />
            <Route path="/my-enrollments" element={<MyEnrollmentsPage />} />
          </Route>

          <Route element={<RequireAuth role="LECTURER" />}>
            <Route path="/teaching" element={<TeachingPage />} />
            <Route path="/teaching/new" element={<SubjectFormPage />} />
            <Route path="/teaching/:id/edit" element={<SubjectFormPage />} />
            <Route path="/teaching/:id/students" element={<ClassListPage />} />
          </Route>
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
