import {
  Alert, Button, Card, Col, Descriptions, Form, Input, InputNumber, Modal,
  Row, Select, Space, Statistic, Table, Tag, message,
} from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useRef, useState } from 'react';
import { masterApi } from '../api/client';
import { PaymentPreview } from '../components/PaymentPreview';
import type { Borrowing } from '../types';
import { calcLoanFinancials } from '../utils/interestCalc';
import { fmtDate, fmtMoney, todayStr } from '../utils/format';

export default function BorrowingsPage() {
  const [form] = Form.useForm();
  const [payForm] = Form.useForm();
  const qc = useQueryClient();
  const [selected, setSelected] = useState<Borrowing | null>(null);
  const [payType, setPayType] = useState<'principal' | 'interest' | null>(null);
  const paymentSectionRef = useRef<HTMLDivElement>(null);
  const payAmount = Form.useWatch('amount', payForm);
  const payDate = Form.useWatch('date', payForm) ?? todayStr();
  const selectedAtDate = useMemo(() => selected
    ? calcLoanFinancials(
      selected.amount,
      selected.date,
      selected.interestRate,
      selected.interestType,
      selected.transactions,
      payDate,
    )
    : null, [selected, payDate]);

  useEffect(() => {
    if (payType === 'interest' && selectedAtDate) {
      payForm.setFieldValue('amount', selectedAtDate.interestBalance);
    }
  }, [payType, selectedAtDate, payForm]);

  useEffect(() => {
    if (payType) {
      requestAnimationFrame(() => paymentSectionRef.current?.scrollIntoView({
        behavior: 'smooth', block: 'nearest',
      }));
    }
  }, [payType]);

  const { data: borrowings = [], refetch, isError } = useQuery({
    queryKey: ['borrowings'],
    queryFn: () => masterApi.borrowings(),
  });
  const { data: lenders = [], isError: lendersError } = useQuery({
    queryKey: ['lenders'],
    queryFn: () => masterApi.lenders(),
  });

  const create = useMutation({
    mutationFn: masterApi.createBorrowing,
    onSuccess: () => {
      message.success('Borrowing created');
      form.resetFields();
      refetch();
      qc.invalidateQueries({ queryKey: ['dashboard'] });
    },
    onError: (e) => message.error(String(e)),
  });

  const pay = useMutation({
    mutationFn: ({ id, type, data }: { id: number; type: string; data: object }) =>
      type === 'principal' ? masterApi.payBorrowingPrincipal(id, data) : masterApi.payBorrowingInterest(id, data),
    onSuccess: async (updated) => {
      message.success('Payment recorded');
      setPayType(null);
      payForm.resetFields();
      setSelected(updated);
      refetch();
    },
    onError: (e) => message.error(String(e)),
  });

  const close = useMutation({
    mutationFn: masterApi.closeBorrowing,
    onSuccess: () => { message.success('Borrowing closed'); setSelected(null); refetch(); },
    onError: (e) => message.error(String(e)),
  });

  const listColumns = [
    { title: 'ID', dataIndex: 'borrowingId', width: 100 },
    { title: 'Lender', dataIndex: 'lenderName' },
    { title: 'Amount', dataIndex: 'amount', render: (v: number) => fmtMoney(v) },
    { title: 'Outstanding', render: (_: unknown, r: Borrowing) => fmtMoney(r.financials.currentPrincipal) },
    { title: 'Interest Due', render: (_: unknown, r: Borrowing) => fmtMoney(r.financials.interestBalance) },
    { title: 'Status', dataIndex: 'status', render: (s: string) => <Tag color={s === 'active' ? 'blue' : 'default'}>{s}</Tag> },
  ];

  const stmtColumns = [
    { title: 'Date', dataIndex: 'date', render: (d: string) => fmtDate(d) },
    { title: 'Type', dataIndex: 'type', render: (t: string) => <Tag>{t}</Tag> },
    {
      title: 'Credit (+)', render: (_: unknown, r: { type: string; amount: number }) =>
        r.type === 'interest' || r.type === 'borrow' ? fmtMoney(r.amount) : '—',
    },
    {
      title: 'Debit (-)', render: (_: unknown, r: { type: string; amount: number }) =>
        r.type === 'principal' ? fmtMoney(r.amount) : '—',
    },
    { title: 'Note', dataIndex: 'note' },
  ];

  const totalOwed = selected
    ? selected.financials.currentPrincipal + selected.financials.interestBalance
    : 0;

  return (
    <>
      <h2 className="page-title">Borrowings</h2>

      {(isError || lendersError) && (
        <Alert type="error" message="Failed to load data. Start the backend first (port 8082) — run backend\run-backend.ps1" style={{ marginBottom: 16 }} showIcon />
      )}

      <Card title="New Borrowing" style={{ marginBottom: 24 }}>
        <Form form={form} layout="vertical" onFinish={(v) => create.mutate(v)}
          initialValues={{ date: todayStr(), interestType: 'monthly', interestRate: 1.2 }}>
          <Row gutter={[16, 0]}>
            <Col xs={24} sm={12} md={8} lg={6}>
              <Form.Item name="lenderId" label="Lender" rules={[{ required: true }]}>
                <Select placeholder="Select lender" showSearch optionFilterProp="label"
                  options={lenders.map((l) => ({ value: l.id, label: `${l.name} (${l.phone})` }))} />
              </Form.Item>
            </Col>
            <Col xs={24} sm={12} md={8} lg={4}>
              <Form.Item name="amount" label="Amount (₹)" rules={[{ required: true }]}>
                <InputNumber min={0} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col xs={24} sm={12} md={8} lg={4}>
              <Form.Item name="date" label="Date" rules={[{ required: true }]}>
                <Input type="date" />
              </Form.Item>
            </Col>
            <Col xs={24} sm={12} md={8} lg={4}>
              <Form.Item name="interestRate" label="Rate (%)" rules={[{ required: true }]}>
                <InputNumber min={0} step={0.1} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col xs={24} sm={12} md={8} lg={3}>
              <Form.Item name="interestType" label="Type">
                <Select options={[{ value: 'monthly', label: 'Monthly' }, { value: 'daily', label: 'Daily' }]} />
              </Form.Item>
            </Col>
            <Col xs={24} sm={12} md={8} lg={3}>
              <Form.Item name="notes" label="Notes"><Input /></Form.Item>
            </Col>
          </Row>
          <Button type="primary" htmlType="submit" loading={create.isPending}>Add Borrowing</Button>
        </Form>
      </Card>

      <Table
        columns={listColumns}
        dataSource={borrowings}
        rowKey="id"
        onRow={(r) => ({ onClick: () => setSelected(r), style: { cursor: 'pointer' } })}
      />

      <Modal
        open={!!selected}
        title={selected ? `Borrowing ${selected.borrowingId} — ${selected.lenderName}` : ''}
        onCancel={() => { setSelected(null); setPayType(null); }}
        width={720}
        centered
        styles={{ body: { maxHeight: 'calc(100vh - 140px)', overflowY: 'auto' } }}
        footer={null}
      >
        {selected && (
          <>
            <Row gutter={16} style={{ marginBottom: 20 }}>
              <Col span={8}><Statistic title="Outstanding Principal" value={fmtMoney(selected.financials.currentPrincipal)} /></Col>
              <Col span={8}><Statistic title="Interest Due" value={fmtMoney(selected.financials.interestBalance)} valueStyle={{ color: '#cf1322' }} /></Col>
              <Col span={8}><Statistic title="Total to Close" value={fmtMoney(totalOwed)} valueStyle={{ color: '#1677ff' }} /></Col>
            </Row>

            <Descriptions bordered size="small" column={2} style={{ marginBottom: 16 }}>
              <Descriptions.Item label="Borrowed">{fmtMoney(selected.amount)}</Descriptions.Item>
              <Descriptions.Item label="Rate">{selected.interestRate}% {selected.interestType}</Descriptions.Item>
              <Descriptions.Item label="Date">{fmtDate(selected.date)}</Descriptions.Item>
              <Descriptions.Item label="Status"><Tag>{selected.status}</Tag></Descriptions.Item>
            </Descriptions>

            <Space style={{ marginBottom: 16 }}>
              <Button type="primary" onClick={() => { setPayType('principal'); payForm.setFieldsValue({ amount: undefined, date: todayStr() }); }}
                disabled={selected.status !== 'active'}>Pay Principal</Button>
              <Button onClick={() => { setPayType('interest'); payForm.setFieldsValue({ amount: selected.financials.interestBalance, date: todayStr() }); }}
                disabled={selected.status !== 'active'}>Pay Interest</Button>
              <Button danger onClick={() => close.mutate(selected.id)} disabled={selected.status !== 'active'}>Close</Button>
            </Space>

            {payType && (
              <div ref={paymentSectionRef}>
              <Card size="small" title={`Pay ${payType}`} style={{ marginBottom: 16, background: '#fafafa' }}>
                <Form form={payForm} layout="vertical"
                  onFinish={(v) => pay.mutate({ id: selected.id, type: payType, data: v })}>
                  <Row gutter={12} align="bottom">
                    <Col span={8}>
                      <Form.Item name="amount" label="Amount" rules={[{ required: true }]}>
                        <InputNumber
                          min={0.01}
                          max={payType === 'principal' ? selectedAtDate?.currentPrincipal : selectedAtDate?.interestBalance}
                          style={{ width: '100%' }}
                          placeholder={payType === 'principal' ? 'Enter part or full principal' : undefined}
                        />
                      </Form.Item>
                    </Col>
                    <Col span={8}><Form.Item name="date" label="Date"><Input type="date" min={selected.date} /></Form.Item></Col>
                    <Col span={8}><Form.Item name="note" label="Note"><Input /></Form.Item></Col>
                  </Row>
                  {payAmount > 0 && (
                    <PaymentPreview
                      compact
                      type={payType}
                      amount={payAmount}
                      outstandingPrincipal={selectedAtDate?.currentPrincipal}
                      interestDue={selectedAtDate?.interestBalance}
                      periodInterest={selectedAtDate?.cycleInterestAmount}
                      periodLabel={selected.interestType === 'monthly' ? '1 Calendar Month Interest' : '30-Day Interest'}
                    />
                  )}
                  <Space>
                    <Button type="primary" htmlType="submit" loading={pay.isPending}>Confirm Payment</Button>
                    <Button onClick={() => setPayType(null)}>Cancel</Button>
                  </Space>
                </Form>
              </Card>
              </div>
            )}

            <Table size="small" columns={stmtColumns} pagination={false} rowKey="id"
              dataSource={[
                { id: 'init', date: selected.date, type: 'borrow', amount: selected.amount, note: 'Initial borrowing' },
                ...selected.transactions,
              ]} />
          </>
        )}
      </Modal>
    </>
  );
}
