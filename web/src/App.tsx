import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './auth/AuthProvider'
import { GuestOnly } from './auth/GuestOnly'
import { RequireAuth } from './auth/RequireAuth'
import { RequireRole } from './auth/RequireRole'
import { AppShell } from './layout/AppShell'
import { AdminDashboard } from './pages/admin/AdminDashboard'
import { AdminDispatch } from './pages/admin/AdminDispatch'
import { AdminHistory } from './pages/admin/AdminHistory'
import { AdminManage } from './pages/admin/manage/AdminManage'
import { AutoAssignTab } from './pages/admin/manage/AutoAssignTab'
import { CatalogTab } from './pages/admin/manage/CatalogTab'
import { UsersTab } from './pages/admin/manage/UsersTab'
import { AdminTechnicianDetail } from './pages/admin/AdminTechnicianDetail'
import { AdminTechnicians } from './pages/admin/AdminTechnicians'
import { ForgotPassword } from './pages/auth/ForgotPassword'
import { Login } from './pages/auth/Login'
import { Register } from './pages/auth/Register'
import { VerifyOtp } from './pages/auth/VerifyOtp'
import { Home } from './pages/Home'
import { StudentCreate } from './pages/student/StudentCreate'
import { StudentFeed } from './pages/student/StudentFeed'
import { StudentTickets } from './pages/student/StudentTickets'
import { Profile } from './pages/Profile'
import { TechHistory } from './pages/technician/TechHistory'
import { TechJobs } from './pages/technician/TechJobs'
import { TechRadar } from './pages/technician/TechRadar'

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          {/* Chưa đăng nhập */}
          <Route element={<GuestOnly />}>
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            <Route path="/forgot" element={<ForgotPassword />} />
          </Route>
          <Route path="/verify" element={<VerifyOtp />} />

          {/* Đã đăng nhập: có khung chung */}
          <Route element={<RequireAuth />}>
            <Route element={<AppShell />}>
              <Route path="/" element={<Home />} />
              <Route path="/profile" element={<Profile />} />

              <Route element={<RequireRole role="student" />}>
                <Route path="/student" element={<StudentFeed />} />
                <Route path="/student/create" element={<StudentCreate />} />
                <Route path="/student/tickets" element={<StudentTickets />} />
              </Route>

              <Route element={<RequireRole role="admin" />}>
                <Route path="/admin" element={<AdminDashboard />} />
                <Route path="/admin/dispatch" element={<AdminDispatch />} />
                <Route path="/admin/history" element={<AdminHistory />} />
                <Route path="/admin/technicians" element={<AdminTechnicians />} />
                <Route path="/admin/technicians/:id" element={<AdminTechnicianDetail />} />
                <Route path="/admin/manage" element={<AdminManage />}>
                  <Route index element={<Navigate to="areas" replace />} />
                  <Route path="areas" element={<CatalogTab key="area" kind="area" />} />
                  <Route path="categories" element={<CatalogTab key="category" kind="category" />} />
                  <Route path="users" element={<UsersTab />} />
                  <Route path="auto" element={<AutoAssignTab />} />
                </Route>
              </Route>

              <Route element={<RequireRole role="technician" />}>
                <Route path="/technician" element={<TechRadar />} />
                <Route path="/technician/jobs" element={<TechJobs />} />
                <Route path="/technician/history" element={<TechHistory />} />
              </Route>
            </Route>
          </Route>

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}
