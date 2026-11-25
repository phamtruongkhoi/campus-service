import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './auth/AuthProvider'
import { GuestOnly } from './auth/GuestOnly'
import { RequireAuth } from './auth/RequireAuth'
import { RequireRole } from './auth/RequireRole'
import { AppShell } from './layout/AppShell'
import { AdminDashboard } from './pages/admin/AdminDashboard'
import { AdminDispatch } from './pages/admin/AdminDispatch'
import { AdminHistory } from './pages/admin/AdminHistory'
import { AdminTechnicianDetail } from './pages/admin/AdminTechnicianDetail'
import { AdminTechnicians } from './pages/admin/AdminTechnicians'
import { ForgotPassword } from './pages/auth/ForgotPassword'
import { Login } from './pages/auth/Login'
import { Register } from './pages/auth/Register'
import { VerifyOtp } from './pages/auth/VerifyOtp'
import { Home } from './pages/Home'
import { Placeholder } from './pages/Placeholder'
import { Profile } from './pages/Profile'

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
                <Route
                  path="/student"
                  element={
                    <Placeholder
                      title="Sinh viên"
                      note="Bảng tin, tạo phiếu, lịch sử và nghiệm thu sẽ làm ở mục 6.1."
                    />
                  }
                />
              </Route>

              <Route element={<RequireRole role="admin" />}>
                <Route path="/admin" element={<AdminDashboard />} />
                <Route path="/admin/dispatch" element={<AdminDispatch />} />
                <Route path="/admin/history" element={<AdminHistory />} />
                <Route path="/admin/technicians" element={<AdminTechnicians />} />
                <Route path="/admin/technicians/:id" element={<AdminTechnicianDetail />} />
              </Route>

              <Route element={<RequireRole role="technician" />}>
                <Route
                  path="/technician"
                  element={
                    <Placeholder
                      title="Kỹ thuật viên"
                      note="Radar, công việc và lịch sử sẽ làm ở mục 6.2."
                    />
                  }
                />
              </Route>
            </Route>
          </Route>

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}
