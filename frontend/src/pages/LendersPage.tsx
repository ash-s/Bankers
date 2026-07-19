import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons';
import { Alert, Button, Form, Input, InputNumber, Modal, Popconfirm, Space, Table, message } from 'antd';
import { useMutation, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { masterApi } from '../api/client';
import type { Lender } from '../types';

export default function LendersPage() {
  const [search, setSearch] = useState('');
  const [editItem, setEditItem] = useState<Lender | null>(null);
  const [addOpen, setAddOpen] = useState(false);
  const [form] = Form.useForm();

  const { data: list = [], refetch, isLoading, isError, error } = useQuery({
    queryKey: ['lenders', search],
    queryFn: () => masterApi.lenders(search || undefined),
  });

  const save = useMutation({
    mutationFn: (v: { id?: number; data: object }) =>
      v.id ? masterApi.updateLender(v.id, v.data) : masterApi.createLender(v.data),
    onSuccess: () => {
      message.success('Saved');
      setEditItem(null);
      setAddOpen(false);
      form.resetFields();
      refetch();
    },
    onError: (e) => message.error(String(e)),
  });

  const del = useMutation({
    mutationFn: masterApi.deleteLender,
    onSuccess: () => { message.success('Deleted'); refetch(); },
    onError: (e) => message.error(String(e)),
  });

  const columns = [
    { title: 'Code', dataIndex: 'code', width: 100 },
    { title: 'Name', dataIndex: 'name' },
    { title: 'Phone', dataIndex: 'phone' },
    { title: 'Address', dataIndex: 'address' },
    { title: 'Default Rate', dataIndex: 'defaultRate', render: (v: number) => `${v}%` },
    {
      title: 'Actions',
      render: (_: unknown, r: Lender) => (
        <Space>
          <Button size="small" icon={<EditOutlined />} onClick={() => { setEditItem(r); form.setFieldsValue(r); }} />
          <Popconfirm title="Delete lender?" onConfirm={() => del.mutate(r.id)}>
            <Button size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2 className="page-title" style={{ margin: 0 }}>Lenders</h2>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setAddOpen(true)}>Add Lender</Button>
      </div>
      {isError && (
        <Alert type="error" showIcon message="Could not load lenders" description={String(error)} style={{ marginBottom: 16 }} />
      )}
      <Input.Search placeholder="Search…" onSearch={setSearch} style={{ maxWidth: 320, marginBottom: 16 }} allowClear />
      <Table columns={columns} dataSource={list} rowKey="id" loading={isLoading} />

      <LenderModal open={addOpen || !!editItem} title={editItem ? 'Edit Lender' : 'Add Lender'}
        onClose={() => { setAddOpen(false); setEditItem(null); form.resetFields(); }}
        form={form} loading={save.isPending}
        onFinish={(v) => save.mutate({ id: editItem?.id, data: v })} />
    </>
  );
}

function LenderModal({ open, title, onClose, form, loading, onFinish }: {
  open: boolean; title: string; onClose: () => void;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  form: any; loading: boolean; onFinish: (v: object) => void;
}) {
  return (
    <Modal open={open} title={title} onCancel={onClose} onOk={() => form.submit()} confirmLoading={loading}>
      <Form form={form} layout="vertical" onFinish={onFinish}>
        <Form.Item name="name" label="Name" rules={[{ required: true }]}><Input /></Form.Item>
        <Form.Item name="phone" label="Phone" rules={[{ required: true }]}><Input /></Form.Item>
        <Form.Item name="address" label="Address"><Input.TextArea rows={2} /></Form.Item>
        <Form.Item name="defaultRate" label="Default Rate (%)"><InputNumber min={0} step={0.1} style={{ width: '100%' }} /></Form.Item>
      </Form>
    </Modal>
  );
}
