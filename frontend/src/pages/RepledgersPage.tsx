import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons';
import { Button, Form, Input, Modal, Popconfirm, Space, Table, message } from 'antd';
import { useMutation, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { masterApi } from '../api/client';
import type { Repledger } from '../types';

export default function RepledgersPage() {
  const [search, setSearch] = useState('');
  const [editItem, setEditItem] = useState<Repledger | null>(null);
  const [addOpen, setAddOpen] = useState(false);
  const [form] = Form.useForm();

  const { data: list = [], refetch } = useQuery({
    queryKey: ['repledgers', search],
    queryFn: () => masterApi.repledgers(search || undefined),
  });

  const save = useMutation({
    mutationFn: (v: { id?: number; data: object }) =>
      v.id ? masterApi.updateRepledger(v.id, v.data) : masterApi.createRepledger(v.data),
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
    mutationFn: masterApi.deleteRepledger,
    onSuccess: () => { message.success('Deleted'); refetch(); },
    onError: (e) => message.error(String(e)),
  });

  const columns = [
    { title: 'Name', dataIndex: 'name' },
    { title: 'Role', dataIndex: 'role' },
    { title: 'Phone', dataIndex: 'phone' },
    {
      title: 'Actions',
      render: (_: unknown, r: Repledger) => (
        <Space>
          <Button size="small" icon={<EditOutlined />} onClick={() => { setEditItem(r); form.setFieldsValue(r); }} />
          <Popconfirm title="Delete?" onConfirm={() => del.mutate(r.id)}>
            <Button size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2 className="page-title" style={{ margin: 0 }}>Repledgers</h2>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setAddOpen(true)}>Add Repledger</Button>
      </div>
      <Input.Search placeholder="Search…" onSearch={setSearch} style={{ maxWidth: 320, marginBottom: 16 }} allowClear />
      <Table columns={columns} dataSource={list} rowKey="id" />

      <Modal open={addOpen || !!editItem} title={editItem ? 'Edit Repledger' : 'Add Repledger'}
        onCancel={() => { setAddOpen(false); setEditItem(null); form.resetFields(); }}
        onOk={() => form.submit()} confirmLoading={save.isPending}>
        <Form form={form} layout="vertical" onFinish={(v) => save.mutate({ id: editItem?.id, data: v })}>
          <Form.Item name="name" label="Name" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="role" label="Role" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="phone" label="Phone" rules={[{ required: true }]}><Input /></Form.Item>
        </Form>
      </Modal>
    </>
  );
}
