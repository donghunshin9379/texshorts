import axios from "axios";
import React from "react";

const adminApi = axios.create({
  baseURL: "http://localhost:8080/api/admin", // 관리자용 API 엔드포인트 예시
});

// 요청 인터셉터로 JWT 헤더 자동 추가
adminApi.interceptors.request.use((config) => {
  const token = localStorage.getItem("jwt");

  // 🔧 headers가 undefined일 경우를 대비해 기본 객체 할당
  config.headers = config.headers || {};

  if (token) {
    config.headers["Authorization"] = `Bearer ${token}`;
  }

  return config;
});


export default adminApi;
