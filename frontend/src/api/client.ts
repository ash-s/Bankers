import type {
  AppNotification, Borrowing, Customer, CustomerListItem, Dashboard,
  InHandSummary, Lender, NotificationSettings, Place, RepledgeDetail,
  RepledgeListItem, Repledger, Settings,
} from '../types';

const API = '/api';

function getToken() {
  return localStorage.getItem('pb_token');
}

export function isTokenExpired(token = getToken()): boolean {
  if (!token) return true;
  try {
    const part = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    const payload = JSON.parse(atob(part.padEnd(Math.ceil(part.length / 4) * 4, '=')));
    return typeof payload.exp !== 'number' || payload.exp * 1000 <= Date.now();
  } catch {
    return true;
  }
}

export function setToken(token: string) {
  localStorage.setItem('pb_token', token);
}

export function clearToken() {
  localStorage.removeItem('pb_token');
}

export async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers: Record<string, string> = {
    ...(options.headers as Record<string, string>),
  };
  if (!(options.body instanceof FormData)) {
    headers['Content-Type'] = 'application/json';
  }
  const token = getToken();
  if (token) headers.Authorization = `Bearer ${token}`;

  const signal = options.signal ?? AbortSignal.timeout(15000);
  const res = await fetch(`${API}${path}`, { ...options, headers, signal });
  if (res.status === 401 || res.status === 403) {
    clearToken();
    localStorage.removeItem('pb_user');
    window.location.href = '/login';
    throw new Error('Unauthorized');
  }
  if (!res.ok) {
    let err = res.statusText;
    try {
      const j = await res.json();
      err = j.error || j.message || JSON.stringify(j);
    } catch {
      err = await res.text() || err;
    }
    throw new Error(err);
  }
  if (res.status === 204) return undefined as T;
  return res.json();
}

function networkError(err: unknown): Error {
  if (err instanceof DOMException && err.name === 'TimeoutError') {
    return new Error('Backend request timed out');
  }
  if (err instanceof TypeError && err.message === 'Failed to fetch') {
    return new Error('Failed to fetch');
  }
  if (err instanceof Error) return err;
  return new Error(String(err));
}

export async function apiSafe<T>(path: string, options: RequestInit = {}): Promise<T> {
  try {
    return await api<T>(path, options);
  } catch (e) {
    throw networkError(e);
  }
}

export async function apiBlob(path: string): Promise<Blob> {
  const token = getToken();
  let res: Response;
  try {
    res = await fetch(`${API}${path}`, {
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    });
  } catch (e) {
    throw networkError(e);
  }
  if (res.status === 401 || res.status === 403) {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    window.location.href = '/login';
    throw new Error('Session expired');
  }
  if (!res.ok) {
    let msg = `Download failed (${res.status})`;
    try {
      const body = await res.json() as { error?: string; message?: string };
      msg = body.error ?? body.message ?? msg;
    } catch {
      try {
        const text = await res.text();
        if (text) msg = text;
      } catch { /* ignore */ }
    }
    throw new Error(msg);
  }
  return res.blob();
}

export const authApi = {
  login: (username: string, password: string) =>
    api<{ token: string; username: string; displayName: string; role: string }>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    }),
};

export const dashboardApi = {
  get: () => apiSafe<Dashboard>('/dashboard'),
  health: () => apiSafe<{ status: string }>('/health'),
};

export const notificationApi = {
  list: () => api<AppNotification[]>('/notifications'),
  sync: () => api<{ synced: boolean; count: number }>('/notifications/sync', { method: 'POST' }),
  unreadCount: () => api<{ count: number }>('/notifications/unread-count'),
  markRead: (id: number) => api<void>(`/notifications/${id}/read`, { method: 'POST' }),
  markAllRead: () => api<void>('/notifications/read-all', { method: 'POST' }),
  getSettings: () => api<NotificationSettings>('/notification-settings'),
  updateSettings: (data: NotificationSettings) =>
    api<NotificationSettings>('/notification-settings', { method: 'PUT', body: JSON.stringify(data) }),
};

export const customerApi = {
  list: (search?: string, filter?: string) => {
    const params = new URLSearchParams();
    if (search) params.set('search', search);
    if (filter && filter !== 'all') params.set('filter', filter);
    const q = params.toString();
    return api<CustomerListItem[]>(`/customers${q ? `?${q}` : ''}`);
  },
  get: (id: number) => api<Customer>(`/customers/${id}`),
  update: (id: number, data: object) =>
    api<Customer>(`/customers/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  delete: (id: number) => api<void>(`/customers/${id}`, { method: 'DELETE' }),
  create: (formData: FormData) =>
    api<Customer>('/customers', { method: 'POST', body: formData }),
  addLoan: (id: number, formData: FormData) =>
    api<Customer>(`/customers/${id}/loans`, { method: 'POST', body: formData }),
  payPrincipal: (loanId: string, data: object) =>
    api<Customer>(`/loans/${loanId}/payments/principal`, { method: 'POST', body: JSON.stringify(data) }),
  payInterest: (loanId: string, data: object) =>
    api<Customer>(`/loans/${loanId}/payments/interest`, { method: 'POST', body: JSON.stringify(data) }),
  discount: (loanId: string, data: object) =>
    api<Customer>(`/loans/${loanId}/payments/discount`, { method: 'POST', body: JSON.stringify(data) }),
  repledge: (loanId: string, data: object) =>
    api<Customer>(`/loans/${loanId}/repledge`, { method: 'POST', body: JSON.stringify(data) }),
  removeRepledge: (loanId: string) =>
    api<Customer>(`/loans/${loanId}/repledge`, { method: 'DELETE' }),
  close: (loanId: string) =>
    api<Customer>(`/loans/${loanId}/close`, { method: 'POST' }),
  billPdf: (loanId: string) => apiBlob(`/loans/${encodeURIComponent(loanId)}/bill.pdf`),
};

export const masterApi = {
  places: (search?: string) =>
    api<Place[]>(`/places${search ? `?search=${encodeURIComponent(search)}` : ''}`),
  createPlace: (data: object) => api<Place>('/places', { method: 'POST', body: JSON.stringify(data) }),
  updatePlace: (id: number, data: object) =>
    api<Place>(`/places/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  deletePlace: (id: number) => api<void>(`/places/${id}`, { method: 'DELETE' }),
  repledgers: (search?: string) =>
    api<Repledger[]>(`/repledgers${search ? `?search=${encodeURIComponent(search)}` : ''}`),
  createRepledger: (data: object) =>
    api<Repledger>('/repledgers', { method: 'POST', body: JSON.stringify(data) }),
  updateRepledger: (id: number, data: object) =>
    api<Repledger>(`/repledgers/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  deleteRepledger: (id: number) => api<void>(`/repledgers/${id}`, { method: 'DELETE' }),
  lenders: (search?: string) =>
    api<Lender[]>(`/lenders${search ? `?search=${encodeURIComponent(search)}` : ''}`),
  createLender: (data: object) => api<Lender>('/lenders', { method: 'POST', body: JSON.stringify(data) }),
  updateLender: (id: number, data: object) =>
    api<Lender>(`/lenders/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  deleteLender: (id: number) => api<void>(`/lenders/${id}`, { method: 'DELETE' }),
  settings: () => api<Settings>('/settings'),
  updateSettings: (data: object) => api<Settings>('/settings', { method: 'PUT', body: JSON.stringify(data) }),
  addMaterial: (name: string) =>
    api<Settings>('/materials', { method: 'POST', body: JSON.stringify({ name }) }),
  deleteMaterial: (id: number) => api<Settings>(`/materials/${id}`, { method: 'DELETE' }),
  inHand: () => api<InHandSummary>('/in-hand'),
  addInHand: (data: object) => api<InHandSummary>('/in-hand', { method: 'POST', body: JSON.stringify(data) }),
  resetInHand: () => api<InHandSummary>('/in-hand/reset', { method: 'POST' }),
  repledges: (search?: string) =>
    api<RepledgeListItem[]>(`/repledges${search ? `?search=${encodeURIComponent(search)}` : ''}`),
  vaultDetail: (loanId: string) => api<RepledgeDetail>(`/repledges/${loanId}`),
  payBankPrincipal: (loanId: string, data: object) =>
    api<RepledgeDetail>(`/repledges/${loanId}/payments/principal`, { method: 'POST', body: JSON.stringify(data) }),
  payBankInterest: (loanId: string, data: object) =>
    api<RepledgeDetail>(`/repledges/${loanId}/payments/interest`, { method: 'POST', body: JSON.stringify(data) }),
  borrowings: (search?: string) =>
    api<Borrowing[]>(`/borrowings${search ? `?search=${encodeURIComponent(search)}` : ''}`),
  borrowing: (id: number) => api<Borrowing>(`/borrowings/${id}`),
  createBorrowing: (data: object) =>
    api<Borrowing>('/borrowings', { method: 'POST', body: JSON.stringify(data) }),
  payBorrowingPrincipal: (id: number, data: object) =>
    api<Borrowing>(`/borrowings/${id}/payments/principal`, { method: 'POST', body: JSON.stringify(data) }),
  payBorrowingInterest: (id: number, data: object) =>
    api<Borrowing>(`/borrowings/${id}/payments/interest`, { method: 'POST', body: JSON.stringify(data) }),
  closeBorrowing: (id: number) => api<Borrowing>(`/borrowings/${id}/close`, { method: 'POST' }),
};
