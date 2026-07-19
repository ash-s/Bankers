import {
  BankOutlined, BellOutlined, DashboardOutlined, DollarOutlined,
  LogoutOutlined, SettingOutlined, ShopOutlined, TeamOutlined,
  UserAddOutlined, UserOutlined, WalletOutlined,
} from '@ant-design/icons';
import { Badge, Button, Dropdown, Layout, List, Menu, Modal, Space, Typography } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useEffect, useRef, useState } from 'react';
import { notificationApi } from '../api/client';
import { useAuth } from '../context/AuthContext';
import type { AppNotification } from '../types';
import dayjs from 'dayjs';

const { Header, Sider, Content } = Layout;

const menuItems = [
  { key: '/', icon: <DashboardOutlined />, label: 'Dashboard' },
  { key: '/add-customer', icon: <UserAddOutlined />, label: 'Add Customer' },
  { key: '/customers', icon: <TeamOutlined />, label: 'All Customers' },
  { key: '/repledge', icon: <BankOutlined />, label: 'Repledge Mgmt' },
  { key: '/banks', icon: <ShopOutlined />, label: 'Banks & Shops' },
  { key: '/repledgers', icon: <UserOutlined />, label: 'Repledgers' },
  { key: '/lenders', icon: <DollarOutlined />, label: 'Lenders' },
  { key: '/borrowings', icon: <WalletOutlined />, label: 'Borrowings' },
  { key: '/in-hand', icon: <WalletOutlined />, label: 'In Hand' },
  { key: '/settings', icon: <SettingOutlined />, label: 'Settings' },
];

export default function LayoutShell() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const qc = useQueryClient();
  const [startupOpen, setStartupOpen] = useState(false);
  const synced = useRef(false);

  useEffect(() => {
    if (synced.current) return;
    synced.current = true;
    notificationApi.sync()
      .then(() => {
        qc.invalidateQueries({ queryKey: ['notifications'] });
        qc.invalidateQueries({ queryKey: ['notifications-unread'] });
      })
      .catch(() => { /* backend may still be starting */ });
  }, [qc]);

  const { data: notifications = [] } = useQuery({
    queryKey: ['notifications'],
    queryFn: notificationApi.list,
    retry: 3,
    refetchOnWindowFocus: true,
    refetchInterval: 120000,
  });

  const { data: unread } = useQuery({
    queryKey: ['notifications-unread'],
    queryFn: notificationApi.unreadCount,
    retry: 3,
    refetchOnWindowFocus: true,
    refetchInterval: 120000,
  });

  const unreadList = notifications.filter((n) => !n.read);

  useEffect(() => {
    if (unreadList.length > 0) setStartupOpen(true);
  }, [unread?.count]);

  const markRead = useMutation({
    mutationFn: notificationApi.markRead,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['notifications'] });
      qc.invalidateQueries({ queryKey: ['notifications-unread'] });
    },
  });

  const markAllRead = useMutation({
    mutationFn: notificationApi.markAllRead,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['notifications'] });
      qc.invalidateQueries({ queryKey: ['notifications-unread'] });
      setStartupOpen(false);
    },
  });

  const notifMenu = {
    items: [
      {
        key: 'header',
        label: (
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', minWidth: 280 }}>
            <strong>Notifications</strong>
            <Button type="link" size="small" onClick={() => markAllRead.mutate()}>Mark all read</Button>
          </div>
        ),
        disabled: true,
      },
      ...notifications.slice(0, 10).map((n: AppNotification) => ({
        key: String(n.id),
        label: (
          <div style={{ opacity: n.read ? 0.6 : 1, maxWidth: 320 }} onClick={() => {
            if (!n.read) markRead.mutate(n.id);
            navigate(`/customers/${n.customerId}`);
          }}>
            <div style={{ fontWeight: n.read ? 400 : 600 }}>{n.customerName} — {n.item}</div>
            <div style={{ fontSize: 12, color: '#888' }}>
              ₹{Number(n.interestBalance).toLocaleString('en-IN')} due · {n.overdueCycles} cycles
            </div>
          </div>
        ),
      })),
      ...(notifications.length === 0 ? [{ key: 'empty', label: 'No notifications', disabled: true }] : []),
    ],
  };

  return (
    <Layout className="app-layout">
      <Sider width={240} theme="dark" breakpoint="lg" collapsedWidth={0}>
        <div className="app-logo">
          <BankOutlined style={{ fontSize: 20 }} />
          Prakash Bankers
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[location.pathname === '/' ? '/' : location.pathname]}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
        />
      </Sider>
      <Layout>
        <Header className="app-header">
          <Typography.Text type="secondary">{dayjs().format('dddd, D MMMM YYYY')}</Typography.Text>
          <Space>
            <Dropdown menu={notifMenu} trigger={['click']} placement="bottomRight">
              <Badge count={unread?.count ?? 0} size="small">
                <Button type="text" icon={<BellOutlined style={{ fontSize: 18 }} />} />
              </Badge>
            </Dropdown>
            <Typography.Text>{user?.displayName}</Typography.Text>
            <Button type="text" icon={<LogoutOutlined />} onClick={() => { logout(); navigate('/login'); }}>
              Logout
            </Button>
          </Space>
        </Header>
        <Content className="app-content">
          <Outlet />
        </Content>
      </Layout>

      <Modal
        title="⚠️ Overdue Interest Alerts"
        open={startupOpen && unreadList.length > 0}
        onCancel={() => setStartupOpen(false)}
        footer={[
          <Button key="dismiss" onClick={() => markAllRead.mutate()}>Mark all read</Button>,
          <Button key="ok" type="primary" onClick={() => setStartupOpen(false)}>Review later</Button>,
        ]}
        width={520}
      >
        <List
          dataSource={unreadList.slice(0, 8)}
          renderItem={(n) => (
            <List.Item
              actions={[
                <Button type="link" key="view" onClick={() => {
                  markRead.mutate(n.id);
                  setStartupOpen(false);
                  navigate(`/customers/${n.customerId}`);
                }}>View</Button>,
              ]}
            >
              <List.Item.Meta
                title={`${n.customerName} — ${n.item}`}
                description={`₹${Number(n.interestBalance).toLocaleString('en-IN')} due · ${n.overdueCycles} overdue cycles`}
              />
            </List.Item>
          )}
        />
      </Modal>
    </Layout>
  );
}
