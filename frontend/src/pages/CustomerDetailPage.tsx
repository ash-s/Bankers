import {
  ArrowLeftOutlined, DownloadOutlined, FilePdfOutlined, PlusOutlined, PrinterOutlined,
} from '@ant-design/icons';
import {
  Button, Card, Col, Collapse, Descriptions, Form, Input, InputNumber, Modal,
  Popconfirm, Row, Select, Space, Table, Tag, Upload, message,
} from 'antd';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { QueryState } from '../components/QueryState';
import { customerApi, masterApi } from '../api/client';
import { PaymentPreview } from '../components/PaymentPreview';
import { InterestCalcPanel } from '../components/InterestCalcPanel';
import type { Loan } from '../types';
import { calcLoanFinancials } from '../utils/interestCalc';
import { fmtDate, fmtMoney, todayStr } from '../utils/format';

export default function CustomerDetailPage() {
  const { id } = useParams<{ id: string }>();
  const [searchParams] = useSearchParams();
  const highlightLoanId = searchParams.get('loan');
  const navigate = useNavigate();
  const qc = useQueryClient();
  const [payModal, setPayModal] = useState<{ loan: Loan; type: 'principal' | 'interest' | 'discount' } | null>(null);
  const [repledgeModal, setRepledgeModal] = useState<Loan | null>(null);
  const [addLoanOpen, setAddLoanOpen] = useState(false);
  const [billPreview, setBillPreview] = useState<{ loanId: string; url: string } | null>(null);
  const [billLoading, setBillLoading] = useState(false);
  const billFrameRef = useRef<HTMLIFrameElement>(null);

  const { data: customer, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['customer', id],
    queryFn: () => customerApi.get(Number(id)),
    enabled: !!id,
  });

  const { data: places = [] } = useQuery({ queryKey: ['places'], queryFn: () => masterApi.places() });
  const { data: repledgers = [] } = useQuery({ queryKey: ['repledgers'], queryFn: () => masterApi.repledgers() });
  const { data: settings } = useQuery({ queryKey: ['settings'], queryFn: masterApi.settings });

  const invalidate = () => {
    qc.invalidateQueries({ queryKey: ['customer', id] });
    qc.invalidateQueries({ queryKey: ['dashboard'] });
  };

  const downloadBill = async (loanId: string) => {
    setBillLoading(true);
    try {
      const blob = await customerApi.billPdf(loanId);
      const url = URL.createObjectURL(blob);
      setBillPreview((current) => {
        if (current) URL.revokeObjectURL(current.url);
        return { loanId, url };
      });
    } catch (e) {
      message.error(String(e).replace(/^Error:\s*/, ''));
    } finally {
      setBillLoading(false);
    }
  };

  const closeBillPreview = () => {
    if (billPreview) URL.revokeObjectURL(billPreview.url);
    setBillPreview(null);
  };

  const saveBill = () => {
    if (!billPreview) return;
    const a = document.createElement('a');
    a.href = billPreview.url;
    a.download = `bill-${billPreview.loanId}.pdf`;
    a.click();
  };

  if (!id) return null;

  return (
    <QueryState isLoading={isLoading} isError={isError} error={error} onRetry={() => refetch()} loadingText="Loading customer…">
      {customer && (
        <CustomerDetailContent
          customer={customer}
          highlightLoanId={highlightLoanId}
          navigate={navigate}
          places={places}
          repledgers={repledgers}
          settings={settings}
          invalidate={invalidate}
          payModal={payModal}
          setPayModal={setPayModal}
          repledgeModal={repledgeModal}
          setRepledgeModal={setRepledgeModal}
          addLoanOpen={addLoanOpen}
          setAddLoanOpen={setAddLoanOpen}
          downloadBill={downloadBill}
          billLoading={billLoading}
        />
      )}
      <Modal
        open={!!billPreview}
        title={billPreview ? `Bill preview — ${billPreview.loanId}` : 'Bill preview'}
        onCancel={closeBillPreview}
        centered
        width="min(920px, 95vw)"
        styles={{ body: { padding: 0, height: 'min(70vh, calc(100vh - 190px))' } }}
        footer={[
          <Button key="close" onClick={closeBillPreview}>Close</Button>,
          <Button key="download" icon={<DownloadOutlined />} onClick={saveBill}>Download PDF</Button>,
          <Button
            key="print"
            type="primary"
            icon={<PrinterOutlined />}
            onClick={() => billFrameRef.current?.contentWindow?.print()}
          >
            Print
          </Button>,
        ]}
      >
        {billPreview && (
          <iframe
            ref={billFrameRef}
            src={billPreview.url}
            title={`Bill ${billPreview.loanId}`}
            style={{ width: '100%', height: '100%', border: 0 }}
          />
        )}
      </Modal>
    </QueryState>
  );
}

function CustomerDetailContent({ customer, highlightLoanId, navigate, places, repledgers, settings, invalidate, payModal, setPayModal, repledgeModal, setRepledgeModal, addLoanOpen, setAddLoanOpen, downloadBill, billLoading }: {
  customer: NonNullable<Awaited<ReturnType<typeof customerApi.get>>>;
  highlightLoanId: string | null;
  navigate: ReturnType<typeof useNavigate>;
  places: { id: number; name: string; defaultRate: number }[];
  repledgers: { id: number; name: string }[];
  settings: { materials: string[] } | undefined;
  invalidate: () => void;
  payModal: { loan: Loan; type: 'principal' | 'interest' | 'discount' } | null;
  setPayModal: (v: typeof payModal) => void;
  repledgeModal: Loan | null;
  setRepledgeModal: (v: Loan | null) => void;
  addLoanOpen: boolean;
  setAddLoanOpen: (v: boolean) => void;
  downloadBill: (loanId: string) => Promise<void>;
  billLoading: boolean;
}) {
  const totalPrincipal = customer.loans.reduce((s, l) => s + (l.status !== 'completed' ? l.financials.currentPrincipal : 0), 0);
  const totalInterest = customer.loans.reduce((s, l) => s + (l.status !== 'completed' ? l.financials.interestBalance : 0), 0);
  const totalWeight = customer.loans.reduce((s, l) => s + (l.status !== 'completed' ? (l.netWeight || 0) : 0), 0);
  const activeLoans = customer.loans.filter((l) => l.status !== 'completed');
  const closedLoans = customer.loans.filter((l) => l.status === 'completed');

  return (
    <>
      <Space style={{ marginBottom: 16 }}>
        <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/customers')}>Back</Button>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setAddLoanOpen(true)}>Add Loan</Button>
      </Space>

      <Card className="customer-header-card" title={`${customer.name}`} extra={<Tag color="blue">{customer.code}</Tag>} style={{ marginBottom: 16 }}>
        <Row gutter={[16, 12]}>
          <Col xs={12} sm={6}><div className="loan-meta-item"><label>Phone</label><span>{customer.phone}</span></div></Col>
          <Col xs={12} sm={6}><div className="loan-meta-item"><label>Address</label><span>{customer.address || '—'}</span></div></Col>
          <Col xs={12} sm={6}><div className="loan-meta-item"><label>ID Proof</label><span>{customer.idProof || '—'}</span></div></Col>
          <Col xs={12} sm={6}><div className="loan-meta-item"><label>ID Document</label><span>
            {customer.hasIdProofFile ? <a href={customer.idProofUrl} target="_blank" rel="noreferrer">View</a> : '—'}
          </span></div></Col>
          <Col xs={12} sm={6}><div className="loan-meta-item"><label>Active Principal</label><span style={{ color: '#1677ff' }}>{fmtMoney(totalPrincipal)}</span></div></Col>
          <Col xs={12} sm={6}><div className="loan-meta-item"><label>Interest Due</label><span style={{ color: '#cf1322' }}>{fmtMoney(totalInterest)}</span></div></Col>
          <Col xs={12} sm={6}><div className="loan-meta-item"><label>Net Weight</label><span>{totalWeight}g</span></div></Col>
          <Col xs={12} sm={6}><div className="loan-meta-item"><label>Active Loans</label><span>{activeLoans.length}</span></div></Col>
        </Row>
      </Card>

      {activeLoans.map((loan) => (
        <LoanCard
          key={loan.loanId}
          loan={loan}
          highlighted={loan.loanId === highlightLoanId}
          onPay={(type) => setPayModal({ loan, type })}
          onRepledge={() => setRepledgeModal(loan)}
          onClose={async () => {
            await customerApi.close(loan.loanId);
            message.success('Account closed');
            invalidate();
          }}
          onRemoveRepledge={async () => {
            await customerApi.removeRepledge(loan.loanId);
            message.success('Repledge removed');
            invalidate();
          }}
          onBill={() => downloadBill(loan.loanId)}
          billLoading={billLoading}
        />
      ))}

      {closedLoans.length > 0 && (
        <Collapse
          style={{ marginBottom: 16 }}
          items={[{
            key: 'closed',
            label: <span><Tag color="default">CLOSED</Tag> Closed Accounts ({closedLoans.length}) — click to expand</span>,
            children: closedLoans.map((loan) => (
              <LoanCard
                key={loan.loanId}
                loan={loan}
                compact
                onPay={() => {}}
                onRepledge={() => {}}
                onClose={async () => {}}
                onRemoveRepledge={async () => {}}
                onBill={() => downloadBill(loan.loanId)}
                billLoading={billLoading}
              />
            )),
          }]}
        />
      )}

      {payModal && (
        <PaymentModal
          loan={payModal.loan}
          type={payModal.type}
          onClose={() => setPayModal(null)}
          onDone={() => { setPayModal(null); invalidate(); }}
        />
      )}

      {repledgeModal && (
        <RepledgeModal
          loan={repledgeModal}
          places={places}
          repledgers={repledgers}
          onClose={() => setRepledgeModal(null)}
          onDone={() => { setRepledgeModal(null); invalidate(); }}
        />
      )}

      {addLoanOpen && (
        <AddLoanModal
          customerId={customer.id}
          materials={settings?.materials ?? []}
          onClose={() => setAddLoanOpen(false)}
          onDone={() => { setAddLoanOpen(false); invalidate(); }}
        />
      )}
    </>
  );
}

function LoanCard({ loan, compact, highlighted, onPay, onRepledge, onClose, onRemoveRepledge, onBill, billLoading }: {
  loan: Loan;
  compact?: boolean;
  highlighted?: boolean;
  onPay: (type: 'principal' | 'interest' | 'discount') => void;
  onRepledge: () => void;
  onClose: () => void;
  onRemoveRepledge: () => void;
  onBill: () => void;
  billLoading?: boolean;
}) {
  const ref = useRef<HTMLDivElement>(null);
  const isRepledged = loan.status === 'repledged';
  const isClosed = loan.status === 'completed';
  const statusColor = isClosed ? 'default' : isRepledged ? 'orange' : 'green';

  useEffect(() => {
    if (highlighted && ref.current) {
      ref.current.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
  }, [highlighted]);

  const txColumns = [
    { title: 'Date', dataIndex: 'date', render: (d: string) => fmtDate(d) },
    { title: 'Type', dataIndex: 'type', render: (t: string) => <Tag>{t}</Tag> },
    { title: 'Amount', dataIndex: 'amount', render: (a: number) => fmtMoney(a) },
    { title: 'Note', dataIndex: 'note' },
  ];

  return (
    <div ref={ref} id={`loan-${loan.loanId}`}>
    <Card
      className={`loan-card ${highlighted ? 'loan-card-highlight' : ''}`}
      size={compact ? 'small' : 'default'}
      title={<Space>{loan.item} <Tag color={statusColor}>{loan.status.toUpperCase()}</Tag> {isRepledged && <Tag color="orange">REPLEDGED</Tag>}</Space>}
      style={{ marginBottom: compact ? 8 : 16, border: isRepledged ? '2px solid #fa8c16' : undefined, opacity: isClosed ? 0.85 : 1 }}
      extra={<Space><Tag>Pledged {fmtDate(loan.pledgeDate)}</Tag><Tag>{loan.loanId}</Tag></Space>}
    >
      {!compact && (
        <>
          <div className="loan-meta-row">
            <div className="loan-meta-item"><label>Pledge Date</label><span>{fmtDate(loan.pledgeDate)}</span></div>
            <div className="loan-meta-item"><label>Material</label><span>{loan.material}</span></div>
            <div className="loan-meta-item"><label>Purity</label><span>{loan.purity}</span></div>
            <div className="loan-meta-item"><label>Gross / Net</label><span>{loan.grossWeight}g / {loan.netWeight}g</span></div>
            <div className="loan-meta-item"><label>Rate</label><span>{loan.interestRate}% {loan.interestType}</span></div>
            <div className="loan-meta-item"><label>Pledge Amount</label><span>{fmtMoney(loan.pledgeAmount)}</span></div>
            <div className="loan-meta-item"><label>Principal Due</label><span style={{ color: '#1677ff' }}>{fmtMoney(loan.financials.currentPrincipal)}</span></div>
            <div className="loan-meta-item"><label>Interest Due</label><span style={{ color: '#cf1322' }}>{fmtMoney(loan.financials.interestBalance)}</span></div>
            {loan.financials.advanceInterestCredit > 0 && (
              <div className="loan-meta-item"><label>Prepaid Interest Credit</label><span style={{ color: '#389e0d' }}>{fmtMoney(loan.financials.advanceInterestCredit)}</span></div>
            )}
          </div>

          {loan.hasMaterialPhoto && (
            <div style={{ marginBottom: 12 }}>
              <a href={loan.materialPhotoUrl} target="_blank" rel="noreferrer">View material photo</a>
            </div>
          )}

          {loan.repledge && (
            <Descriptions size="small" bordered style={{ marginBottom: 16 }} title="Repledge Info">
              <Descriptions.Item label="Bank">{loan.repledge.placeName}</Descriptions.Item>
              <Descriptions.Item label="Staff">{loan.repledge.repledgerName}</Descriptions.Item>
              <Descriptions.Item label="Borrowed">{fmtMoney(loan.repledge.amount)}</Descriptions.Item>
              <Descriptions.Item label="Rate">{loan.repledge.rate}% {loan.repledge.interestType}</Descriptions.Item>
              <Descriptions.Item label="Date">{fmtDate(loan.repledge.date)}</Descriptions.Item>
            </Descriptions>
          )}

          <Space wrap style={{ marginBottom: 16 }}>
            <Button onClick={() => onPay('principal')} disabled={isClosed}>Pay Principal</Button>
            <Button onClick={() => onPay('interest')} disabled={isClosed}>Pay Interest</Button>
            <Button onClick={() => onPay('discount')} disabled={isClosed}>Discount Interest</Button>
            <Button type="primary" onClick={onRepledge} disabled={isClosed}>
              {loan.repledge ? 'Edit Repledge' : 'Repledge Item'}
            </Button>
            {loan.repledge && (
              <Popconfirm title="Remove repledge?" onConfirm={onRemoveRepledge}>
                <Button danger>Remove Repledge</Button>
              </Popconfirm>
            )}
            <Popconfirm title="Close this account?" onConfirm={onClose}>
              <Button danger disabled={isClosed}>Close Account</Button>
            </Popconfirm>
            <Button icon={<FilePdfOutlined />} loading={billLoading} onClick={onBill}>Preview Bill</Button>
          </Space>
        </>
      )}

      {compact && (
        <Space style={{ marginBottom: 8 }} wrap>
          <span>Pledged: {fmtDate(loan.pledgeDate)}</span>
          <span>Principal: {fmtMoney(loan.financials.currentPrincipal)}</span>
          <span>Interest: {fmtMoney(loan.financials.interestBalance)}</span>
          <Button size="small" icon={<FilePdfOutlined />} loading={billLoading} onClick={onBill}>Bill</Button>
        </Space>
      )}

      <Table size="small" columns={txColumns} dataSource={loan.transactions} rowKey="id" pagination={compact ? { pageSize: 3 } : false} />
    </Card>
    </div>
  );
}

function PaymentModal({ loan, type, onClose, onDone }: {
  loan: Loan; type: 'principal' | 'interest' | 'discount'; onClose: () => void; onDone: () => void;
}) {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const payAmount = Form.useWatch('amount', form);
  const payDate = Form.useWatch('date', form) ?? todayStr();

  const finAtDate = useMemo(() => calcLoanFinancials(
    loan.pledgeAmount,
    loan.pledgeDate,
    loan.interestRate,
    loan.interestType,
    loan.transactions,
    payDate,
  ), [loan, payDate]);

  useEffect(() => {
    if (type !== 'principal') {
      form.setFieldValue('amount', finAtDate.interestBalance);
    }
  }, [payDate, type, finAtDate.currentPrincipal, finAtDate.interestBalance, form]);

  const submit = async (v: { amount: number; date: string; note?: string }) => {
    setLoading(true);
    try {
      const fn = type === 'principal' ? customerApi.payPrincipal
        : type === 'interest' ? customerApi.payInterest : customerApi.discount;
      await fn(loan.loanId, { amount: v.amount, date: v.date, note: v.note });
      message.success('Payment recorded');
      onDone();
    } catch (e) {
      message.error(String(e));
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      open
      title={`${type.charAt(0).toUpperCase() + type.slice(1)} payment — ${loan.item}`}
      onCancel={onClose}
      onOk={() => form.submit()}
      confirmLoading={loading}
      okText="Confirm Payment"
      width={720}
      centered
      styles={{ body: { maxHeight: 'calc(100vh - 190px)', overflowY: 'auto' } }}
    >
      <PaymentPreview
        compact
        type={type}
        amount={payAmount}
        outstandingPrincipal={finAtDate.currentPrincipal}
        interestDue={finAtDate.interestBalance}
        periodInterest={finAtDate.cycleInterestAmount}
        periodLabel={loan.interestType === 'monthly' ? '1 Calendar Month Interest' : '30-Day Interest'}
        label={payDate !== todayStr() ? `As of ${fmtDate(payDate)}` : undefined}
      />
      <Descriptions size="small" bordered column={2} style={{ marginBottom: 12 }}>
        <Descriptions.Item label="Generated to selected date">
          {fmtMoney(finAtDate.interestGenerated)}
        </Descriptions.Item>
        <Descriptions.Item label="Interest paid / discounted">
          {fmtMoney(finAtDate.interestPaid + finAtDate.discountGiven)}
        </Descriptions.Item>
      </Descriptions>
      {finAtDate.advanceInterestCredit > 0 && (
        <Tag color="green" style={{ marginBottom: 12 }}>
          Prepaid interest credit: {fmtMoney(finAtDate.advanceInterestCredit)}
        </Tag>
      )}
      <Form
        form={form}
        layout="vertical"
        onFinish={submit}
        initialValues={{ date: todayStr(), amount: type === 'principal' ? undefined : finAtDate.interestBalance }}
      >
        <Form.Item name="amount" label="Amount (₹)" rules={[{ required: true }]}>
          <InputNumber
            min={0.01}
            max={type === 'principal' ? finAtDate.currentPrincipal : finAtDate.interestBalance}
            style={{ width: '100%' }}
            placeholder={type === 'principal' ? 'Enter part or full principal amount' : undefined}
          />
        </Form.Item>
        <Form.Item name="date" label="Date" rules={[{ required: true }]}>
          <Input type="date" min={loan.pledgeDate} />
        </Form.Item>
        <Form.Item name="note" label="Note"><Input placeholder="Optional note" /></Form.Item>
      </Form>
      {finAtDate.interestBreakdown.length > 0 && (
        <Collapse
          size="small"
          defaultActiveKey={type === 'interest' ? ['interest-breakdown'] : []}
          items={[{
            key: 'interest-breakdown',
            label: `Interest calculation breakdown (${finAtDate.interestBreakdown.length})`,
            children: (
              <Table
                size="small"
                pagination={{ pageSize: 5 }}
                rowKey={(r) => `${r.fromDate}-${r.toDate}-${r.principal}`}
                dataSource={finAtDate.interestBreakdown}
                columns={[
                  { title: 'Period', render: (_: unknown, r: typeof finAtDate.interestBreakdown[number]) => `${fmtDate(r.fromDate)} – ${fmtDate(r.toDate)}` },
                  { title: 'Principal', dataIndex: 'principal', render: fmtMoney },
                  { title: 'Rate', dataIndex: 'rate', render: (v: number) => `${v}%` },
                  {
                    title: 'Calendar days',
                    render: (_: unknown, r: typeof finAtDate.interestBreakdown[number]) => r.interestType === 'monthly' ? `${r.days}/${r.cycleDays}` : `${r.days}`,
                  },
                  { title: 'Interest', dataIndex: 'interest', render: fmtMoney },
                ]}
              />
            ),
          }]}
        />
      )}
    </Modal>
  );
}

function RepledgeModal({ loan, places, repledgers, onClose, onDone }: {
  loan: Loan; places: { id: number; name: string; defaultRate: number }[];
  repledgers: { id: number; name: string }[];
  onClose: () => void; onDone: () => void;
}) {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const existing = loan.repledge;

  const submit = async (v: Record<string, unknown>) => {
    setLoading(true);
    try {
      await customerApi.repledge(loan.loanId, v);
      message.success('Repledge saved');
      onDone();
    } catch (e) {
      message.error(String(e));
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal open centered title={`Repledge — ${loan.item}`} onCancel={onClose} onOk={() => form.submit()} confirmLoading={loading} width={520}
      styles={{ body: { maxHeight: 'calc(100vh - 190px)', overflowY: 'auto' } }}>
      <Form form={form} layout="vertical" onFinish={submit}
        initialValues={existing ? {
          placeId: existing.placeId, repledgerId: existing.repledgerId,
          date: existing.date, amount: existing.amount, rate: existing.rate,
          interestType: existing.interestType,
        } : { date: todayStr(), amount: loan.financials.currentPrincipal, rate: 1.5, interestType: loan.interestType }}>
        <Form.Item name="placeId" label="Bank / Shop" rules={[{ required: true }]}>
          <Select options={places.map((p) => ({ value: p.id, label: p.name }))} />
        </Form.Item>
        <Form.Item name="repledgerId" label="Staff (Repledger)" rules={[{ required: true }]}>
          <Select options={repledgers.map((r) => ({ value: r.id, label: r.name }))} />
        </Form.Item>
        <Form.Item name="date" label="Repledge Date" rules={[{ required: true }]}>
          <Input type="date" />
        </Form.Item>
        <Form.Item name="amount" label="Principal Borrowed" rules={[{ required: true }]}>
          <InputNumber min={0} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="rate" label="Interest Rate (%)" rules={[{ required: true }]}>
          <InputNumber min={0} step={0.1} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="interestType" label="Interest Type" rules={[{ required: true }]}>
          <Select options={[{ value: 'monthly', label: 'Monthly' }, { value: 'daily', label: 'Daily' }]} />
        </Form.Item>
      </Form>
    </Modal>
  );
}

function AddLoanModal({ customerId, materials, onClose, onDone }: {
  customerId: number; materials: string[]; onClose: () => void; onDone: () => void;
}) {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [photo, setPhoto] = useState<File | null>(null);
  const [prepaidFirstPeriod, setPrepaidFirstPeriod] = useState(false);

  const pledgeAmount = Form.useWatch('pledgeAmount', form);
  const pledgeDate = Form.useWatch('pledgeDate', form);
  const interestRate = Form.useWatch('interestRate', form);
  const interestType = Form.useWatch('interestType', form) ?? 'monthly';

  const submit = async (v: Record<string, string | number>) => {
    setLoading(true);
    try {
      const fd = new FormData();
      Object.entries(v).forEach(([k, val]) => fd.append(k, String(val)));
      fd.append('prepaidFirstPeriod', String(prepaidFirstPeriod));
      if (photo) fd.append('materialPhoto', photo);
      await customerApi.addLoan(customerId, fd);
      message.success('Loan added');
      onDone();
    } catch (e) {
      message.error(String(e));
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal open centered title="Add New Loan" onCancel={onClose} onOk={() => form.submit()} confirmLoading={loading} width={600}
      styles={{ body: { maxHeight: 'calc(100vh - 190px)', overflowY: 'auto' } }}>
      <Form form={form} layout="vertical" onFinish={submit} initialValues={{ pledgeDate: todayStr(), interestType: 'monthly', interestRate: 2 }}>
        <Row gutter={12}>
          <Col span={12}><Form.Item name="material" label="Material" rules={[{ required: true }]}>
            <Select options={materials.map((m) => ({ value: m, label: m }))} />
          </Form.Item></Col>
          <Col span={12}><Form.Item name="item" label="Item Description" rules={[{ required: true }]}><Input /></Form.Item></Col>
        </Row>
        <Row gutter={12}>
          <Col span={8}><Form.Item name="grossWeight" label="Gross (g)" rules={[{ required: true }]}><InputNumber min={0} style={{ width: '100%' }} /></Form.Item></Col>
          <Col span={8}><Form.Item name="netWeight" label="Net (g)" rules={[{ required: true }]}><InputNumber min={0} style={{ width: '100%' }} /></Form.Item></Col>
          <Col span={8}><Form.Item name="purity" label="Purity" rules={[{ required: true }]}><Input placeholder="22K" /></Form.Item></Col>
        </Row>
        <Row gutter={12}>
          <Col span={8}><Form.Item name="pledgeAmount" label="Amount (₹)" rules={[{ required: true }]}><InputNumber min={0} style={{ width: '100%' }} /></Form.Item></Col>
          <Col span={8}><Form.Item name="pledgeDate" label="Date" rules={[{ required: true }]}><Input type="date" /></Form.Item></Col>
          <Col span={8}><Form.Item name="interestRate" label="Rate %" rules={[{ required: true }]}><InputNumber min={0} step={0.1} style={{ width: '100%' }} /></Form.Item></Col>
        </Row>
        <Form.Item name="interestType" label="Interest Type"><Select options={[{ value: 'monthly', label: 'Monthly' }, { value: 'daily', label: 'Daily' }]} /></Form.Item>
        {pledgeAmount > 0 && (
          <InterestCalcPanel
            pledgeAmount={pledgeAmount}
            pledgeDate={pledgeDate}
            interestRate={interestRate}
            interestType={interestType}
            prepaidFirstPeriod={prepaidFirstPeriod}
            onPrepaidChange={setPrepaidFirstPeriod}
          />
        )}
        <Form.Item label="Material Photo">
          <Upload beforeUpload={(f) => { setPhoto(f); return false; }} maxCount={1} accept="image/*,.pdf">
            <Button>Upload Photo</Button>
          </Upload>
        </Form.Item>
      </Form>
    </Modal>
  );
}
