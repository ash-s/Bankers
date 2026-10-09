import { DeleteOutlined, EditOutlined, PlusOutlined, SearchOutlined } from '@ant-design/icons';
import { Button, Form, Input, Modal, Popconfirm, Space, Table, Tag, message } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { customerApi } from '../api/client';
import type { CustomerListItem } from '../types';
import { fmtMoney } from '../utils/format';

const FILTER_TITLES: Record<string, string> = {
  all: 'All Customers',
  overdue: 'Overdue Customers',
  active: 'Active Customers',
  vault: 'Gold in Vault',
  repledged: 'Repledged Customers',
};

export default function CustomerListPage() {
  const [search, setSearch] = useState('');
  const [editItem, setEditItem] = useState<CustomerListItem | null>(null);
  const [form] = Form.useForm();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const filter = searchParams.get('filter') ?? 'all';
  const qc = useQueryClient();

  const { data: customers = [], isLoading, refetch } = useQuery({
    queryKey: ['customers', search, filter],
    queryFn: () => customerApi.list(search || undefined, filter),
  });

  const save = useMutation({
    mutationFn: ({ id, data }: { id: number; data: object }) => customerApi.update(id, data),
    onSuccess: () => {
      message.success('Customer updated');
      setEditItem(null);
      form.resetFields();
      refetch();
    },
    onError: (e) => message.error(String(e)),
  });

  const del = useMutation({
    mutationFn: customerApi.delete,
    onSuccess: () => {
      message.success('Customer deleted');
      refetch();
      qc.invalidateQueries({ queryKey: ['dashboard'] });
    },
    onError: (e) => message.error(String(e)),
  });

  const columns = [
    { title: 'Code', dataIndex: 'code', width: 110 },
    { title: 'Name', dataIndex: 'name' },
    { title: 'Phone', dataIndex: 'phone', width: 130 },
    {
      title: 'Loans',
      width: 90,
      render: (_: unknown, r: CustomerListItem) => (
        <Tag color="blue">{r.activeLoanCount}/{r.loanCount}</Tag>
      ),
    },
    {
      title: 'Due',
      width: 140,
      render: (_: unknown, r: CustomerListItem) => (
        r.activeLoanCount > 0
          ? <span style={{ color: '#cf1322' }}>{fmtMoney(r.totalInterestDue)}</span>
          : '—'
      ),
    },
    {
      title: 'Status',
      width: 100,
      render: (_: unknown, r: CustomerListItem) =>
        r.hasOverdue ? <Tag color="red">Overdue</Tag> : r.activeLoanCount > 0 ? <Tag color="green">Active</Tag> : <Tag>Closed</Tag>,
    },
    {
      title: 'Actions',
      width: 160,
      render: (_: unknown, r: CustomerListItem) => (
        <Space onClick={(e) => e.stopPropagation()}>
          <Button size="small" type="link" onClick={() => navigate(`/customers/${r.id}`)}>View</Button>
          <Button size="small" icon={<EditOutlined />} onClick={() => { setEditItem(r); form.setFieldsValue(r); }} />
          <Popconfirm title="Delete customer?" description="Only allowed when all loans are closed." onConfirm={() => del.mutate(r.id)}>
            <Button size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <div>
          <h2 className="page-title" style={{ margin: 0 }}>{FILTER_TITLES[filter] ?? 'All Customers'}</h2>
          {filter !== 'all' && (
            <Button type="link" size="small" style={{ padding: 0 }} onClick={() => navigate('/customers')}>
              ← Show all customers
            </Button>
          )}
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/add-customer')}>Add Customer</Button>
      </div>
      <Space style={{ marginBottom: 16 }}>
        <Input.Search prefix={<SearchOutlined />} placeholder="Search name or phone…" allowClear
          onSearch={setSearch} style={{ width: 320 }} />
      </Space>
      <Table columns={columns} dataSource={customers} rowKey="id" loading={isLoading}
        onRow={(r) => ({ onClick: () => navigate(`/customers/${r.id}`), style: { cursor: 'pointer' } })} />

      <Modal open={!!editItem} title="Edit Customer" onCancel={() => { setEditItem(null); form.resetFields(); }}
        onOk={() => form.submit()} confirmLoading={save.isPending}>
        <Form form={form} layout="vertical" onFinish={(v) => editItem && save.mutate({ id: editItem.id, data: v })}>
          <Form.Item name="name" label="Name" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="phone" label="Phone"><Input /></Form.Item>
          <Form.Item name="address" label="Address"><Input.TextArea rows={2} /></Form.Item>
          <Form.Item name="idProof" label="ID Proof"><Input /></Form.Item>
        </Form>
      </Modal>
    </>
  );
}
