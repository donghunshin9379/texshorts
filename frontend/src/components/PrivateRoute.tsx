import React, { ReactNode } from "react";
import { Navigate } from "react-router-dom";

interface PrivateRouteProps {
  children: ReactNode;
  requiredRole: string;
}

export default function PrivateRoute({ children, requiredRole }: PrivateRouteProps) {
  const roles = JSON.parse(localStorage.getItem("roles") || "[]");

  if (!roles.includes(requiredRole)) {
    return <Navigate to="/login" replace />;
  }

  return <>{children}</>;
}
