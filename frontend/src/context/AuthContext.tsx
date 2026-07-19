import { createContext, useContext, useState, useEffect, type ReactNode } from 'react';
import { authApi, setToken, clearToken, isTokenExpired } from '../api/client';

interface AuthState {
  username: string;
  displayName: string;
  role: string;
}

interface AuthContextType {
  user: AuthState | null;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
  isReady: boolean;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthState | null>(() => {
    if (isTokenExpired()) {
      clearToken();
      localStorage.removeItem('pb_user');
      return null;
    }
    const stored = localStorage.getItem('pb_user');
    if (!stored) return null;
    try {
      return JSON.parse(stored);
    } catch {
      localStorage.removeItem('pb_user');
      return null;
    }
  });
  const [isReady, setIsReady] = useState(true);

  useEffect(() => {
    if (isTokenExpired()) {
      clearToken();
      setUser(null);
      localStorage.removeItem('pb_user');
    }
    setIsReady(true);
  }, []);

  const login = async (username: string, password: string) => {
    const res = await authApi.login(username, password);
    setToken(res.token);
    const u = { username: res.username, displayName: res.displayName, role: res.role };
    localStorage.setItem('pb_user', JSON.stringify(u));
    setUser(u);
  };

  const logout = () => {
    clearToken();
    localStorage.removeItem('pb_user');
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, login, logout, isReady }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
