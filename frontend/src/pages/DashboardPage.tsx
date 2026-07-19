import {
  BankOutlined, DollarOutlined, GoldOutlined, RiseOutlined,
  TeamOutlined, WalletOutlined, FallOutlined, AccountBookOutlined,
} from '@ant-design/icons';
import { Button, Card, Col, Empty, Row, Table, Tag, Typography } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { QueryState } from '../components/QueryState';
import { dashboardApi } from '../api/client';
import { fmtMoney } from '../utils/format';

export default function DashboardPage() {
  const navigate = useNavigate();
  const { data, isLoading, isError, error, refetch, isFetching } = useQuery({
    queryKey: ['dashboard'],
    queryFn: dashboardApi.get,
  });

  return (
    <QueryState
      isLoading={isLoading || (isFetching && !data)}
      isError={isError}
      error={error}
      onRetry={() => refetch()}
      loadingText="Loading dashboard…"
    >
      {data && <DashboardContent data={data} navigate={navigate} />}
    </QueryState>
  );
}

function DashboardContent({ data, navigate }: { data: NonNullable<Awaited<ReturnType<typeof dashboardApi.get>>>; navigate: ReturnType<typeof useNavigate> }) {
  const plPositive = (data.profitLoss ?? 0) >= 0;

  const columns = [
    { title: 'Customer', dataIndex: 'customerName', ellipsis: true },
    { title: 'Item', dataIndex: 'item', ellipsis: true },
    {
      title: 'Interest Due',
      dataIndex: 'interestBalance',
      render: (v: number) => <Typography.Text type="danger" strong>{fmtMoney(v)}</Typography.Text>,
    },
    {
      title: 'Overdue',
      dataIndex: 'overdueCycles',
      render: (v: number) => <Tag color={v >= 2 ? 'red' : 'orange'}>{v} cycles</Tag>,
    },
    {
      title: '',
      width: 80,
      render: (_: unknown, r: { customerId: number; loanId: string }) => (
        <Button type="link" size="small" onClick={() => navigate(`/customers/${r.customerId}?loan=${r.loanId}`)}>View</Button>
      ),
    },
  ];

  const sections = [
    {
      title: 'Cash & Profit',
      kpis: [
        { key: 'in-hand', icon: <WalletOutlined />, color: '#1677ff', title: 'In Hand', value: fmtMoney(data.cashInHandEstimated + data.inHandBalance), sub: 'Live ledger cash', path: '/in-hand' },
        { key: 'pl', icon: plPositive ? <RiseOutlined /> : <FallOutlined />, color: plPositive ? '#52c41a' : '#ff4d4f', title: 'Net Profit / Loss', value: fmtMoney(data.profitLoss ?? 0), sub: 'After bank & lender interest', path: '/in-hand', highlight: !plPositive },
        { key: 'position', icon: <DollarOutlined />, color: '#2f54eb', title: 'Net Position', value: fmtMoney(data.netPosition ?? 0), sub: 'Receivable − payable', path: '/in-hand' },
      ],
    },
    {
      title: 'Receivables & Payables',
      kpis: [
        { key: 'receivable', icon: <AccountBookOutlined />, color: '#722ed1', title: 'Total Receivable', value: fmtMoney(data.totalReceivable ?? 0), sub: 'Principal + interest owed to you', path: '/customers?filter=active' },
        { key: 'payable', icon: <BankOutlined />, color: '#eb2f96', title: 'Total Payable', value: fmtMoney(data.totalPayable ?? 0), sub: 'Banks + lenders', path: '/borrowings' },
        { key: 'overdue', icon: <TeamOutlined />, color: data.overdueCount ? '#ff4d4f' : '#13c2c2', title: 'Overdue Accounts', value: String(data.overdueCount), sub: 'Customers with unpaid interest', path: '/customers?filter=overdue', highlight: data.overdueCount > 0 },
      ],
    },
    {
      title: 'Gold & Lending',
      kpis: [
        { key: 'vault', icon: <GoldOutlined />, color: '#faad14', title: 'Gold in Vault', value: `${data.goldInVaultWeight}g`, sub: `${data.goldInVaultCount} items in shop`, path: '/customers?filter=vault' },
        { key: 'pledged', icon: <BankOutlined />, color: '#13c2c2', title: 'Gold Pledged Out', value: fmtMoney(data.totalActivePrincipal), sub: `Due ${fmtMoney(data.totalActivePrincipal + data.totalPendingInterest)}`, path: '/customers?filter=active' },
        { key: 'repledge', icon: <BankOutlined />, color: '#eb2f96', title: 'Repledged to Banks', value: fmtMoney(data.totalRepledgedPrincipal), sub: `Pay ${fmtMoney(data.totalOwedToBanks)}`, path: '/repledge' },
        { key: 'borrowed', icon: <DollarOutlined />, color: '#2f54eb', title: 'Borrowed from Lenders', value: fmtMoney(data.totalBorrowedOutstanding), sub: `Pay ${fmtMoney(data.totalOwedToLenders)}`, path: '/borrowings' },
      ],
    },
  ];

  return (
    <>
      <div className="dash-header">
        <div>
          <Typography.Title level={3} style={{ margin: 0 }}>Dashboard</Typography.Title>
          <Typography.Text type="secondary">Click any card to open the filtered view</Typography.Text>
        </div>
        <Button type="primary" size="large" onClick={() => navigate('/add-customer')}>+ Add Customer</Button>
      </div>

      {sections.map((section) => (
        <div key={section.title} className="dash-section">
          <Typography.Title level={5} className="dash-section-title">{section.title}</Typography.Title>
          <Row gutter={[16, 16]} style={{ marginBottom: 8 }}>
            {section.kpis.map((k) => (
              <Col xs={24} sm={12} lg={8} key={k.key}>
                <KpiCard {...k} onClick={() => navigate(k.path)} />
              </Col>
            ))}
          </Row>
        </div>
      ))}

      <Card
        title="This Month's Attention"
        extra={data.overdueCount > 0 ? <Tag color="red">{data.overdueCount} overdue</Tag> : <Tag color="green">All clear</Tag>}
        className="dash-attention-card"
        style={{ marginTop: 16 }}
      >
        {(data.attentionList ?? []).length === 0 ? (
          <Empty description="No overdue interest — all caught up" />
        ) : (
          <Table columns={columns} dataSource={data.attentionList} rowKey="loanId" pagination={false} size="middle" />
        )}
      </Card>
    </>
  );
}

function KpiCard({ icon, color, title, value, sub, highlight, onClick }: {
  icon: React.ReactNode; color: string; title: string; value: string;
  sub?: string; highlight?: boolean; onClick?: () => void;
}) {
  return (
    <Card className="kpi-card kpi-card-clickable" styles={{ body: { padding: '18px 20px' } }} onClick={onClick} hoverable>
      <div className="kpi-card-inner">
        <div className="kpi-icon" style={{ background: `${color}18`, color }}>{icon}</div>
        <div className="kpi-card-body">
          <div className="kpi-title">{title}</div>
          <div className={`kpi-value ${highlight ? 'kpi-danger' : ''}`}>{value}</div>
          {sub && <div className="kpi-sub">{sub}</div>}
        </div>
      </div>
    </Card>
  );
}
