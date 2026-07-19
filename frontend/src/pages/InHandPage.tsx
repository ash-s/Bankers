import { Button, Card, Col, Form, Input, InputNumber, Row, Select, Statistic, Table, message } from 'antd';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { QueryState } from '../components/QueryState';
import { dashboardApi, masterApi } from '../api/client';
import { fmtDate, fmtMoney, todayStr } from '../utils/format';

export default function InHandPage() {
  const qc = useQueryClient();
  const { data, refetch, isLoading, isError, error } = useQuery({
    queryKey: ['in-hand'], queryFn: masterApi.inHand, refetchInterval: 30_000,
  });
  const { data: dash } = useQuery({
    queryKey: ['dashboard'], queryFn: dashboardApi.get, refetchInterval: 30_000,
  });

  return (
    <QueryState isLoading={isLoading} isError={isError} error={error} onRetry={() => refetch()} loadingText="Loading in-hand…">
      {data && <InHandContent data={data} dash={dash} refetch={refetch} qc={qc} />}
    </QueryState>
  );
}

function InHandContent({ data, dash, refetch, qc }: {
  data: NonNullable<Awaited<ReturnType<typeof masterApi.inHand>>>;
  dash: Awaited<ReturnType<typeof dashboardApi.get>> | undefined;
  refetch: () => void;
  qc: ReturnType<typeof useQueryClient>;
}) {
  const [form] = Form.useForm();

  const plPositive = (dash?.profitLoss ?? 0) >= 0;

  const columns = [
    { title: 'Date', dataIndex: 'date', render: (d: string) => fmtDate(d) },
    { title: 'Type', dataIndex: 'type' },
    { title: 'Amount', dataIndex: 'amount', render: (a: number, r: { type: string }) => (
      <span style={{ color: r.type === 'take' ? '#cf1322' : '#389e0d' }}>
        {r.type === 'take' ? '-' : '+'}{fmtMoney(a)}
      </span>
    )},
    { title: 'Note', dataIndex: 'note' },
  ];

  return (
    <>
      <h2 className="page-title">In Hand — Live Cash & Profit</h2>

      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={12} lg={6}>
          <Card className="stat-card">
            <Statistic
              title="Live Cash in Hand"
              value={fmtMoney((dash?.cashInHandEstimated ?? 0) + data.balance)}
              valueStyle={{ fontSize: 20, color: '#1677ff' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card className="stat-card">
            <Statistic title="Net Profit / Loss" value={fmtMoney(dash?.profitLoss ?? 0)}
              valueStyle={{ fontSize: 20, color: plPositive ? '#389e0d' : '#cf1322' }} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card className="stat-card">
            <Statistic title="Customer Receivable" value={fmtMoney(dash?.totalReceivable ?? 0)} valueStyle={{ fontSize: 20 }} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card className="stat-card">
            <Statistic title="Bank & Lender Payable" value={fmtMoney(dash?.totalPayable ?? 0)} valueStyle={{ fontSize: 20, color: '#cf1322' }} />
          </Card>
        </Col>
      </Row>

      <Card title="Cash adjustment" style={{ marginBottom: 24 }}>
        <Form form={form} layout="inline" initialValues={{ type: 'put', date: todayStr() }}
          onFinish={async (v) => {
            await masterApi.addInHand(v);
            form.resetFields(['amount', 'note']);
            refetch();
            qc.invalidateQueries({ queryKey: ['dashboard'] });
            message.success('Entry added');
          }}>
          <Form.Item name="type"><Select style={{ width: 100 }} options={[{ value: 'put', label: 'Put' }, { value: 'take', label: 'Take' }]} /></Form.Item>
          <Form.Item name="amount" rules={[{ required: true }]}><InputNumber min={0} placeholder="Amount" /></Form.Item>
          <Form.Item name="date"><Input type="date" /></Form.Item>
          <Form.Item name="note"><Input placeholder="Note" /></Form.Item>
          <Button type="primary" htmlType="submit">Add</Button>
        </Form>
      </Card>

      <Card title="Cash adjustment history">
      <Table columns={columns} dataSource={data.entries} rowKey="id" size="small"
        locale={{ emptyText: 'No entries yet — balance starts at ₹0' }} />
      </Card>
    </>
  );
}
