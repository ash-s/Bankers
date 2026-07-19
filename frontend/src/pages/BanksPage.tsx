import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons';
import { Button, Form, Input, InputNumber, Modal, Popconfirm, Select, Space, Table, message } from 'antd';
import { useMutation, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { masterApi } from '../api/client';
import type { Place } from '../types';
import { fmtMoney } from '../utils/format';

export default function BanksPage() {
  const [search, setSearch] = useState('');
  const [editItem, setEditItem] = useState<Place | null>(null);
  const [addOpen, setAddOpen] = useState(false);
  const [form] = Form.useForm();

  const { data: places = [], refetch } = useQuery({
    queryKey: ['places', search],
    queryFn: () => masterApi.places(search || undefined),
  });

  const save = useMutation({
    mutationFn: (v: { id?: number; data: object }) =>
      v.id ? masterApi.updatePlace(v.id, v.data) : masterApi.createPlace(v.data),
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
    mutationFn: masterApi.deletePlace,
    onSuccess: () => { message.success('Deleted'); refetch(); },
    onError: (e) => message.error(String(e)),
  });

  const columns = [
    { title: 'Name', dataIndex: 'name' },
    { title: 'Type', dataIndex: 'type', render: (t: string) => t?.toUpperCase() },
    { title: 'Contact', dataIndex: 'contact' },
    { title: 'Address', dataIndex: 'address', ellipsis: true },
    { title: 'Rate', dataIndex: 'defaultRate', render: (v: number) => `${v}%` },
    { title: 'Active Items', dataIndex: 'activeItems' },
    { title: 'Liability', dataIndex: 'totalLiability', render: (v: number) => fmtMoney(v) },
    {
      title: 'Actions',
      render: (_: unknown, r: Place) => (
        <Space>
          {/* form.setFieldsValue(r) will automatically use the correct backend casing ('Bank' or 'Shop') */}
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
        <h2 className="page-title" style={{ margin: 0 }}>Banks & Shops</h2>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setAddOpen(true)}>Add Place</Button>
      </div>
      <Input.Search placeholder="Search…" onSearch={setSearch} style={{ maxWidth: 320, marginBottom: 16 }} allowClear />
      <Table columns={columns} dataSource={places} rowKey="id" />

      <PlaceModal open={addOpen || !!editItem} title={editItem ? 'Edit Place' : 'Add Place'}
        onClose={() => { setAddOpen(false); setEditItem(null); form.resetFields(); }}
        form={form} loading={save.isPending}
        onFinish={(v) => save.mutate({ id: editItem?.id, data: v })} />
    </>
  );
}

function PlaceModal({ open, title, onClose, form, loading, onFinish }: {
  open: boolean; title: string; onClose: () => void;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  form: any; loading: boolean; onFinish: (v: object) => void;
}) {
  return (
    <Modal open={open} title={title} onCancel={onClose} onOk={() => form.submit()} confirmLoading={loading} width={520}>
      {/* Updated initialValues and option values to match Java Enum casing ('Bank' and 'Shop') */}
      <Form form={form} layout="vertical" onFinish={onFinish} initialValues={{ type: 'Bank', defaultRate: 1.5 }}>
        <Form.Item name="name" label="Name" rules={[{ required: true }]}><Input /></Form.Item>
        <Form.Item name="type" label="Type" rules={[{ required: true }]}>
          <Select options={[{ value: 'Bank', label: 'Bank' }, { value: 'Shop', label: 'Shop' }]} />
        </Form.Item>
        <Form.Item name="contact" label="Contact"><Input /></Form.Item>
        <Form.Item name="address" label="Address"><Input.TextArea rows={2} /></Form.Item>
        <Form.Item name="defaultRate" label="Default Rate (%)"><InputNumber min={0} step={0.1} style={{ width: '100%' }} /></Form.Item>
      </Form>
    </Modal>
  );
}