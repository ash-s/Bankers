import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, Card, Form, Input, InputNumber, Select, Space, Switch, Tag, message } from 'antd';
import { masterApi, notificationApi } from '../api/client';

export default function SettingsPage() {
  const qc = useQueryClient();
  const [newMat, setNewMat] = useState('');
  const [capitalForm] = Form.useForm();
  const [notifForm] = Form.useForm();
  const [preset, setPreset] = useState('30d');

  const { data: settings, refetch, isLoading: settingsLoading, isError: settingsError } = useQuery({
    queryKey: ['settings'],
    queryFn: masterApi.settings,
  });

  const { data: notifSettings, refetch: refetchNotif, isLoading: notifLoading, isError: notifError } = useQuery({
    queryKey: ['notification-settings'],
    queryFn: notificationApi.getSettings,
  });

  const saveNotif = useMutation({
    mutationFn: notificationApi.updateSettings,
    onSuccess: () => {
      message.success('Notification settings saved');
      refetchNotif();
    },
    onError: (e) => message.error(String(e)),
  });

  useEffect(() => {
    if (notifSettings) {
      setPreset(notifSettings.duePreset ?? '30d');
      notifForm.setFieldsValue(notifSettings);
    }
  }, [notifSettings, notifForm]);

  if (settingsLoading || notifLoading) return <div>Loading settings…</div>;
  if (settingsError || notifError || !settings || !notifSettings) {
    return <Card><p>Could not load settings. Is the backend running?</p></Card>;
  }

  const saveShop = async (v: object) => {
    await masterApi.updateSettings({ ...settings, ...v });
    message.success('Settings saved');
    refetch();
  };

  const notifInitial = { ...notifSettings, duePreset: notifSettings.duePreset ?? '30d' };

  return (
    <>
      <h2 className="page-title">Settings</h2>

      <Card title="Shop Details" style={{ marginBottom: 24 }}>
        <Form layout="vertical" initialValues={settings} onFinish={saveShop}>
          <Form.Item name="shopName" label="Shop Name"><Input /></Form.Item>
          <Form.Item name="shopAddress" label="Address"><Input.TextArea rows={2} /></Form.Item>
          <Form.Item name="shopPhone" label="Phone"><Input /></Form.Item>
          <Button type="primary" htmlType="submit">Save Shop Info</Button>
        </Form>
      </Card>

      <Card title="Notification Settings" style={{ marginBottom: 24 }}>
        <Form
          form={notifForm}
          layout="vertical"
          initialValues={notifInitial}
          onFinish={(v) => saveNotif.mutate(v)}
          onValuesChange={(changed) => {
            if ('duePreset' in changed) setPreset(changed.duePreset as string);
          }}
        >
          <Form.Item name="duePreset" label="Notify when interest is due after">
            <Select
              options={[
                { value: '30d', label: '1 Month (30 days)' },
                { value: '180d', label: '6 Months (180 days)' },
                { value: '365d', label: '1 Year (365 days)' },
                { value: 'custom', label: 'Custom days' },
              ]}
            />
          </Form.Item>
          {(preset === 'custom' || notifForm.getFieldValue('duePreset') === 'custom') && (
            <Form.Item name="customDueDays" label="Custom days" rules={[{ required: true }]}>
              <InputNumber min={1} max={3650} style={{ width: '100%' }} />
            </Form.Item>
          )}
          <Form.Item name="pushEnabled" label="Show popup on app start" valuePropName="checked">
            <Switch />
          </Form.Item>
          <Form.Item name="smsEnabled" label="SMS alerts" valuePropName="checked">
            <Switch />
          </Form.Item>
          <Form.Item name="smsTemplate" label="SMS template">
            <Input.TextArea rows={2} placeholder="Use {name}, {amount}, {item}, {phone}" />
          </Form.Item>
          <Form.Item name="whatsappEnabled" label="WhatsApp alerts" valuePropName="checked">
            <Switch />
          </Form.Item>
          <Form.Item name="whatsappTemplate" label="WhatsApp template">
            <Input.TextArea rows={2} placeholder="Use {name}, {amount}, {item}" />
          </Form.Item>
          <Button type="primary" htmlType="submit" loading={saveNotif.isPending}>Save Notification Settings</Button>
        </Form>
      </Card>

      {!settings.openingCapitalSet && (
        <Card title="Opening Capital (one-time)" style={{ marginBottom: 24 }}>
          <Space>
            <InputNumber min={0} placeholder="Amount" onChange={(v) => capitalForm.setFieldValue('opening', v)} style={{ width: 200 }} />
            <Button type="primary" onClick={async () => {
              const val = capitalForm.getFieldValue('opening');
              if (val) {
                await masterApi.updateSettings({ ...settings, openingCapital: val });
                refetch();
                qc.invalidateQueries({ queryKey: ['in-hand'] });
                qc.invalidateQueries({ queryKey: ['dashboard'] });
                message.success('Opening capital set');
              }
            }}>Set Opening Capital</Button>
          </Space>
        </Card>
      )}

      <Card title="Precious Metals">
        <Space style={{ marginBottom: 16 }}>
          <Input value={newMat} onChange={(e) => setNewMat(e.target.value)} placeholder="e.g. Diamond" style={{ width: 200 }} />
          <Button onClick={async () => {
            if (newMat.trim()) {
              await masterApi.addMaterial(newMat.trim());
              setNewMat('');
              refetch();
            }
          }}>Add Material</Button>
        </Space>
        <Space wrap>
          {settings.materials.map((m) => (
            <Tag key={m}>{m}</Tag>
          ))}
        </Space>
      </Card>
    </>
  );
}
