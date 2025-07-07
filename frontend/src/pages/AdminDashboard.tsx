import React, { useEffect, useState } from "react";
import adminApi from "../api/adminApi";

export default function AdminDashboard() {
  const [visitorCount, setVisitorCount] = useState<number | null>(null);

  useEffect(() => {
    adminApi.get<{ count: number }>("/visitors/count")
      .then(res => setVisitorCount(res.data.count))
      .catch(() => setVisitorCount(null));
  }, []);

  return (
    <div>
      <h1>관리자 대시보드</h1>
      <p>실시간 접속자 수: {visitorCount !== null ? visitorCount : "로딩 중..."}</p>
      {/* 조회수 TOP100, 좋아요 TOP100, 신고된 댓글 등 추가 구현 */}
    </div>
  );
}
