import { EyeOutlined } from '@ant-design/icons';
import {
  Button, Card, Col, Descriptions, Form, Input, InputNumber, Modal,
  Row, Space, Statistic, Table, Tag, message,
} from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { masterApi } from '../api/client';
import { PaymentPreview } from '../components/PaymentPreview';
import type { RepledgeListItem } from '../types';
import { calcLoanFinancials } from '../utils/interestCalc';
import { fmtDate, fmtMoney, todayStr } from '../utils/format';

export default function RepledgePage() {
  const [search, setSearch] = useState('');
  const [selected, setSelected] = useState<string | null>(null);
  const navigate = useNavigate();

  const { data: items = [] } = useQuery({
    queryKey: ['repledges', search],
    queryFn: () => masterApi.repledges(search || undefined),
  });

  const columns = [
    { title: 'Customer', dataIndex: 'customerName', key: 'customer' },
    { title: 'Item', dataIndex: 'item' },
    { title: 'Our Principal', dataIndex: 'customerPrincipal', render: (v: number) => fmtMoney(v) },
    { title: 'Bank Borrowed', dataIndex: 'bankBorrowed', render: (v: number) => fmtMoney(v) },
    { title: 'Bank', dataIndex: 'placeName' },
    {
      title: 'Action',
      render: (_: unknown, r: RepledgeListItem) => (
        <Button type="link" onClick={() => setSelected(r.loanId)}>View Details</Button>
      ),
    },
  ];

  return (
    <>
      <h2 className="page-title">Repledge Management</h2>
      <Input.Search placeholder="Search customer or item…" allowClear onSearch={setSearch} style={{ maxWidth: 360, marginBottom: 16 }} />
      <Table columns={columns} dataSource={items} rowKey="loanId" onRow={(r) => ({
        onClick: () => setSelected(r.loanId),
        style: { cursor: 'pointer' },
      })} />

      <RepledgeModal
        loanId={selected}
        onClose={() => setSelected(null)}
        onViewCustomer={(customerId) => navigate(`/customers/${customerId}?loan=${selected}`)}
      />
    </>
  );
}

function RepledgeModal({ loanId, onClose, onViewCustomer }: {
  loanId: string | null; onClose: () => void; onViewCustomer: (id: number) => void;
}) {
  const qc = useQueryClient();
  const [payType, setPayType] = useState<'principal' | 'interest' | null>(null);
  const paymentSectionRef = useRef<HTMLDivElement>(null);
  const [form] = Form.useForm();
  const payAmount = Form.useWatch('amount', form);
  const payDate = Form.useWatch('date', form) ?? todayStr();

  const { data: list = [] } = useQuery({ queryKey: ['repledges'], queryFn: () => masterApi.repledges() });
  const item = list.find((i) => i.loanId === loanId);

  const { data: detail, refetch } = useQuery({
    queryKey: ['vault', loanId],
    queryFn: () => masterApi.vaultDetail(loanId!),
    enabled: !!loanId,
  });

  const detailAtDate = useMemo(() => detail
    ? calcLoanFinancials(
      detail.amount,
      detail.date,
      detail.rate,
      detail.interestType,
      detail.transactions,
      payDate,
    )
    : null, [detail, payDate]);

  useEffect(() => {
    if (payType === 'interest' && detailAtDate) {
      form.setFieldValue('amount', detailAtDate.interestBalance);
    }
  }, [payType, detailAtDate, form]);

  useEffect(() => {
    if (payType) {
      requestAnimationFrame(() => paymentSectionRef.current?.scrollIntoView({
        behavior: 'smooth', block: 'nearest',
      }));
    }
  }, [payType]);

  const pay = useMutation({
    mutationFn: ({ type, data }: { type: 'principal' | 'interest'; data: object }) =>
      type === 'principal'
        ? masterApi.payBankPrincipal(loanId!, data)
        : masterApi.payBankInterest(loanId!, data),
    onSuccess: () => {
      message.success('Payment recorded');
      setPayType(null);
      form.resetFields();
      refetch();
      qc.invalidateQueries({ queryKey: ['repledges'] });
    },
    onError: (e) => message.error(String(e)),
  });

  const txColumns = [
    { title: 'Date', dataIndex: 'date', render: (d: string) => fmtDate(d) },
    { title: 'Type', dataIndex: 'type', render: (t: string) => <Tag>{t}</Tag> },
    { title: 'Debit/Credit', dataIndex: 'amount', render: (a: number, r: { type: string }) => (
      <span style={{ color: r.type === 'principal' ? '#cf1322' : '#389e0d' }}>
        {r.type === 'interest' ? '+' : '-'}{fmtMoney(a)}
      </span>
    )},
  ];

  const handleClose = () => {
    setPayType(null);
    form.resetFields();
    onClose();
  };

  return (
    <Modal
      open={!!loanId}
      onCancel={handleClose}
      footer={null}
      width={680}
      centered
      styles={{ body: { maxHeight: 'calc(100vh - 140px)', overflowY: 'auto' } }}
      destroyOnClose
      title={item ? `${item.customerName} — ${item.item}` : 'Repledge Details'}
    >
      {detail && item && (
        <>
          <Row gutter={12} style={{ marginBottom: 12 }}>
            <Col span={8}>
              <Statistic title="Bank Principal Due" value={fmtMoney(detail.financials.currentPrincipal)} valueStyle={{ fontSize: 14 }} />
            </Col>
            <Col span={8}>
              <Statistic title="Bank Interest Due" value={fmtMoney(detail.financials.interestBalance)} valueStyle={{ fontSize: 14, color: '#cf1322' }} />
            </Col>
            <Col span={8}>
              <Statistic title="Total to Bank" value={fmtMoney(detail.financials.currentPrincipal + detail.financials.interestBalance)} valueStyle={{ fontSize: 14, color: '#1677ff' }} />
            </Col>
          </Row>

          <Descriptions bordered column={2} size="small" style={{ marginBottom: 12 }}>
            <Descriptions.Item label="Bank">{detail.placeName}</Descriptions.Item>
            <Descriptions.Item label="Staff">{detail.repledgerName}</Descriptions.Item>
            <Descriptions.Item label="Borrowed">{fmtMoney(detail.amount)}</Descriptions.Item>
            <Descriptions.Item label="Rate">{detail.rate}% {detail.interestType}</Descriptions.Item>
            <Descriptions.Item label="Principal Outstanding">{fmtMoney(detail.financials.currentPrincipal)}</Descriptions.Item>
            <Descriptions.Item label="Interest Due">{fmtMoney(detail.financials.interestBalance)}</Descriptions.Item>
            <Descriptions.Item label="Rate Spread">{detail.rateSpread}%</Descriptions.Item>
            <Descriptions.Item label="Repledge Date">{fmtDate(detail.date)}</Descriptions.Item>
          </Descriptions>

          <Space style={{ marginBottom: 12 }}>
            <Button type="primary" size="small" onClick={() => {
              setPayType('principal');
              form.setFieldsValue({ amount: undefined, date: todayStr() });
            }}>Pay Principal</Button>
            <Button size="small" onClick={() => {
              setPayType('interest');
              form.setFieldsValue({ amount: detail.financials.interestBalance, date: todayStr() });
            }}>Pay Interest</Button>
            <Button size="small" type="default" icon={<EyeOutlined />} onClick={() => onViewCustomer(item.customerId)}>
              View Customer Jewelry
            </Button>
          </Space>

          {payType && (
            <div ref={paymentSectionRef}>
            <Card size="small" title={`Pay ${payType} to bank`} style={{ marginBottom: 12, background: '#fafafa' }} styles={{ body: { padding: 12 } }}>
              <PaymentPreview
                compact
                type={payType}
                amount={payAmount}
                outstandingPrincipal={detailAtDate?.currentPrincipal}
                interestDue={detailAtDate?.interestBalance}
                periodInterest={detailAtDate?.cycleInterestAmount}
                periodLabel={detail.interestType === 'monthly' ? '1 Calendar Month Interest' : '30-Day Interest'}
              />
              <Form form={form} layout="vertical" onFinish={(v) => pay.mutate({ type: payType, data: v })}>
                <Row gutter={12}>
                  <Col span={8}>
                    <Form.Item name="amount" label="Amount (₹)" rules={[{ required: true }]}>
                      <InputNumber
                        min={0.01}
                        max={payType === 'principal' ? detailAtDate?.currentPrincipal : detailAtDate?.interestBalance}
                        style={{ width: '100%' }}
                        placeholder={payType === 'principal' ? 'Enter part or full principal' : undefined}
                      />
                    </Form.Item>
                  </Col>
                  <Col span={8}><Form.Item name="date" label="Date"><Input type="date" min={detail.date} /></Form.Item></Col>
                </Row>
                <Space>
                  <Button type="primary" htmlType="submit" loading={pay.isPending}>Confirm Payment</Button>
                  <Button onClick={() => setPayType(null)}>Cancel</Button>
                </Space>
              </Form>
            </Card>
            </div>
          )}

          <Card size="small" title="Bank Payment Ledger" styles={{ body: { padding: 8 } }}>
            <Table size="small" columns={txColumns} dataSource={detail.transactions} rowKey="id" pagination={false} />
          </Card>
        </>
      )}
    </Modal>
  );
}
