// src/pages/AdminLoginPage.tsx
import React, { useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";

interface LoginResponse {
  token: string;
  roles: string[];
}


export default function AdminLoginPage() {
  const navigate = useNavigate();

  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");

    try {
      const response = await axios.post<LoginResponse>("http://localhost:8080/api/auth/login", {
        username,
        password,
      });

      const { token, roles } = response.data;

      localStorage.setItem("jwt", token);
      localStorage.setItem("roles", JSON.stringify(roles));

      if (roles.includes("ROLE_ADMIN")) {
        navigate("/admin/dashboard");
      } else {
        setError("관리자 권한이 없습니다.");
      }
    } catch (err) {
      setError("로그인 실패: 아이디 또는 비밀번호를 확인하세요.");
    }
  };

  return (
    <form onSubmit={handleLogin}>
      <h2>관리자 로그인</h2>
      <input
        type="text"
        value={username}
        onChange={(e) => setUsername(e.target.value)}
        placeholder="아이디"
        required
      />
      <input
        type="password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        placeholder="비밀번호"
        required
      />
      <button type="submit">로그인</button>
      {error && <p style={{ color: "red" }}>{error}</p>}
    </form>
  );
}
