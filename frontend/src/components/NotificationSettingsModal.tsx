import { Form, Input, InputNumber, Modal, Select, Switch, message } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { notificationApi } from '../api/client';

interface Props {
  open: boolean;
  onClose: () => void;
}

export default function NotificationSettingsModal({ open, onClose }: Props) {
  const [form] = Form.useForm();
  const qc = useQueryClient();

  const { data } = useQuery({
    queryKey: ['notification-settings'],
    queryFn: notificationApi.getSettings,
    enabled: open,
  });

  const save = useMutation({
    mutationFn: notificationApi.updateSettings,
    onSuccess: () => {
      message.success('Notification settings saved');
      qc.invalidateQueries({ queryKey: ['notification-settings'] });
      onClose();
    },
  });

  const preset = Form.useWatch('duePreset', form);

  return (
    <Modal
      title="Notification Settings"
      open={open}
      onCancel={onClose}
      onOk={() => form.submit()}
      confirmLoading={save.isPending}
      width={560}
    >
      <Form
        form={form}
        layout="vertical"
        initialValues={data}
        key={data ? JSON.stringify(data) : 'loading'}
        onFinish={(v) => save.mutate(v)}
      >
        <Form.Item name="duePreset" label="Notify when interest is due after">
          <Select options={[
            { value: '30d', label: '1 Month (30 days)' },
            { value: '180d', label: '6 Months (180 days)' },
            { value: '365d', label: '1 Year (365 days)' },
            { value: 'custom', label: 'Custom (manual days)' },
          ]} />
        </Form.Item>
        {preset === 'custom' && (
          <Form.Item name="customDueDays" label="Custom days" rules={[{ required: true }]}>
            <InputNumber min={1} max={3650} style={{ width: '100%' }} placeholder="Enter number of days" />
          </Form.Item>
        )}
        <Form.Item name="pushEnabled" label="Push notifications (on app start)" valuePropName="checked">
          <Switch />
        </Form.Item>
        <Form.Item name="smsEnabled" label="SMS notifications" valuePropName="checked">
          <Switch />
        </Form.Item>
        <Form.Item name="smsTemplate" label="SMS message template">
          <Input.TextArea rows={2} placeholder="Use {name}, {amount}, {item}, {phone}" />
        </Form.Item>
        <Form.Item name="whatsappEnabled" label="WhatsApp notifications" valuePropName="checked">
          <Switch />
        </Form.Item>
        <Form.Item name="whatsappTemplate" label="WhatsApp message template">
          <Input.TextArea rows={2} placeholder="Use {name}, {amount}, {item}" />
        </Form.Item>
      </Form>
    </Modal>
  );
}
