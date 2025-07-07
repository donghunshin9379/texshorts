import React from "react";
import { BrowserRouter as Router, Routes, Route } from "react-router-dom";

import AdminLoginPage from "./pages/AdminLoginPage";
import AdminDashboard from "./pages/AdminDashboard";
import PrivateRoute from "./components/PrivateRoute";

function App() {
  return (
    <Router>
      <Routes>
        {/*  페이지 */}
          {/* 🔹 루트 경로에 바로 관리자 로그인 페이지 연결 */}
                <Route path="/" element={<AdminLoginPage />} />


        {/* 관리자 대시보드 (인증 필요) */}
        <Route
          path="/admin/dashboard"
          element={
            <PrivateRoute requiredRole="ROLE_ADMIN">
              <AdminDashboard />
            </PrivateRoute>
          }
        />

        {/* 필요하면 일반 사용자 페이지 추가 */}
      </Routes>
    </Router>
  );
}

export default App;
