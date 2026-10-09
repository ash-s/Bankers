import { BankOutlined } from '@ant-design/icons';
import { Button, Card, Form, Input, Typography, message } from 'antd';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);

  const onFinish = async (v: { username: string; password: string }) => {
    setLoading(true);
    try {
      await login(v.username, v.password);
      navigate('/');
    } catch (error) {
      const detail = error instanceof Error ? error.message : '';
      message.error(
        detail === 'Failed to fetch' || detail.includes('timed out')
          ? 'Backend is unavailable. Run start-app.ps1 and try again.'
          : 'Invalid username or password',
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      <Card className="login-card">
        <div style={{ textAlign: 'center', marginBottom: 24 }}>
          <BankOutlined style={{ fontSize: 40, color: '#1677ff' }} />
          <Typography.Title level={3} style={{ margin: '8px 0 0' }}>Prakash Bankers</Typography.Title>
          <Typography.Text type="secondary">Pledge & Repledge CRM</Typography.Text>
        </div>
        <Form layout="vertical" onFinish={onFinish} initialValues={{ username: 'admin' }}>
          <Form.Item name="username" label="Username" rules={[{ required: true }]}>
            <Input size="large" />
          </Form.Item>
          <Form.Item name="password" label="Password" rules={[{ required: true }]}>
            <Input.Password size="large" />
          </Form.Item>
          <Button type="primary" htmlType="submit" block size="large" loading={loading}>
            Sign In
          </Button>
        </Form>
      </Card>
    </div>
  );
}
