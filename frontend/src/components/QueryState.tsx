import { Alert, Button, Spin } from 'antd';

interface QueryStateProps {
  isLoading: boolean;
  isError: boolean;
  error?: Error | null;
  onRetry?: () => void;
  loadingText?: string;
  children: React.ReactNode;
}

function formatError(error?: Error | null): string {
  if (!error) return 'Unknown error';
  const msg = error.message || String(error);
  if (msg === 'Failed to fetch' || msg.includes('NetworkError') || msg.includes('ECONNREFUSED')) {
    return 'Cannot reach the backend. Start it with: cd backend && .\\run-backend.ps1 (runs on port 8082).';
  }
  return msg;
}

export function QueryState({
  isLoading,
  isError,
  error,
  onRetry,
  loadingText = 'Loading…',
  children,
}: QueryStateProps) {
  if (isLoading) {
    return (
      <div className="page-loading">
        <Spin size="large" />
        <p>{loadingText}</p>
      </div>
    );
  }

  if (isError) {
    return (
      <Alert
        type="error"
        showIcon
        style={{ marginTop: 24 }}
        message="Failed to load data"
        description={formatError(error)}
        action={onRetry ? <Button size="small" danger onClick={onRetry}>Retry</Button> : undefined}
      />
    );
  }

  return <>{children}</>;
}
