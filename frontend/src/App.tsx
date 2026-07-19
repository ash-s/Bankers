import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { lazy, Suspense } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';

const Layout = lazy(() => import('./components/Layout'));
const LoginPage = lazy(() => import('./pages/LoginPage'));
const DashboardPage = lazy(() => import('./pages/DashboardPage'));
const AddCustomerPage = lazy(() => import('./pages/AddCustomerPage'));
const CustomerListPage = lazy(() => import('./pages/CustomerListPage'));
const CustomerDetailPage = lazy(() => import('./pages/CustomerDetailPage'));
const RepledgePage = lazy(() => import('./pages/RepledgePage'));
const BanksPage = lazy(() => import('./pages/BanksPage'));
const RepledgersPage = lazy(() => import('./pages/RepledgersPage'));
const LendersPage = lazy(() => import('./pages/LendersPage'));
const BorrowingsPage = lazy(() => import('./pages/BorrowingsPage'));
const InHandPage = lazy(() => import('./pages/InHandPage'));
const SettingsPage = lazy(() => import('./pages/SettingsPage'));

const qc = new QueryClient({
  defaultOptions: {
    queries: {
      retry: (failureCount, error) => {
        if (error instanceof Error && error.message === 'Unauthorized') return false;
        return failureCount < 3;
      },
      retryDelay: (attempt) => Math.min(1000 * 2 ** attempt, 8000),
      staleTime: 30000,
      refetchOnWindowFocus: true,
      refetchOnReconnect: true,
    },
    mutations: {
      retry: false,
    },
  },
});

function PrivateRoute({ children }: { children: React.ReactNode }) {
  const { user, isReady } = useAuth();
  if (!isReady) return null;
  if (!user || !localStorage.getItem('pb_token')) return <Navigate to="/login" replace />;
  return <>{children}</>;
}

export default function App() {
  return (
    <QueryClientProvider client={qc}>
      <AuthProvider>
        <BrowserRouter>
          <Suspense fallback={<div className="page-loading">Loading application…</div>}>
            <Routes>
              <Route path="/login" element={<LoginPage />} />
              <Route path="/" element={<PrivateRoute><Layout /></PrivateRoute>}>
                <Route index element={<DashboardPage />} />
                <Route path="add-customer" element={<AddCustomerPage />} />
                <Route path="customers" element={<CustomerListPage />} />
                <Route path="customers/:id" element={<CustomerDetailPage />} />
                <Route path="repledge" element={<RepledgePage />} />
                <Route path="banks" element={<BanksPage />} />
                <Route path="repledgers" element={<RepledgersPage />} />
                <Route path="lenders" element={<LendersPage />} />
                <Route path="borrowings" element={<BorrowingsPage />} />
                <Route path="in-hand" element={<InHandPage />} />
                <Route path="settings" element={<SettingsPage />} />
              </Route>
            </Routes>
          </Suspense>
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  );
}
