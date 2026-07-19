import { Alert, Button, Col, Row, Space, Typography } from 'antd';
import { calcLoanFinancials, cycleInterest, daysSince } from '../utils/interestCalc';
import { fmtMoney, todayStr } from '../utils/format';

interface InterestCalcPanelProps {
  pledgeAmount?: number;
  pledgeDate?: string;
  interestRate?: number;
  interestType?: 'monthly' | 'daily';
  prepaidFirstPeriod: boolean;
  onPrepaidChange: (value: boolean) => void;
}

export function InterestCalcPanel({
  pledgeAmount = 0,
  pledgeDate,
  interestRate = 0,
  interestType = 'monthly',
  prepaidFirstPeriod,
  onPrepaidChange,
}: InterestCalcPanelProps) {
  const periodLabel = interestType === 'monthly' ? '1 calendar month' : '30 days';
  const onePeriod = cycleInterest(pledgeAmount, interestRate, interestType);
  const days = daysSince(pledgeDate ?? '');
  const totalAccrued = pledgeDate
    ? calcLoanFinancials(
      pledgeAmount, pledgeDate, interestRate, interestType, [], todayStr(),
    ).interestGenerated
    : 0;
  const dueNow = Math.max(0, totalAccrued - (prepaidFirstPeriod ? onePeriod : 0));

  return (
    <Alert
      type="info"
      showIcon={false}
      style={{ marginBottom: 16, background: '#f6ffed', border: '1px solid #b7eb8f' }}
      message={<Typography.Text strong>Interest Calculator</Typography.Text>}
      description={
        <>
          <Row gutter={[16, 8]} style={{ marginBottom: 12 }}>
            <Col span={8}>
              <Typography.Text type="secondary">1 period ({periodLabel})</Typography.Text>
              <div><strong>{fmtMoney(onePeriod)}</strong></div>
            </Col>
            <Col span={8}>
              <Typography.Text type="secondary">Days since pledge</Typography.Text>
              <div><strong>{days} days</strong></div>
            </Col>
            <Col span={8}>
              <Typography.Text type="secondary">Interest due now</Typography.Text>
              <div><strong style={{ color: '#cf1322' }}>{fmtMoney(dueNow)}</strong></div>
            </Col>
          </Row>

          <Typography.Text type="secondary" style={{ display: 'block', marginBottom: 8 }}>
            Was {periodLabel} interest already paid at pledge?
          </Typography.Text>
          <Space>
            <Button
              type={prepaidFirstPeriod ? 'primary' : 'default'}
              onClick={() => onPrepaidChange(true)}
            >
              Yes — prepaid
            </Button>
            <Button
              type={!prepaidFirstPeriod ? 'primary' : 'default'}
              danger={!prepaidFirstPeriod}
              onClick={() => onPrepaidChange(false)}
            >
              No — accrue from day 1
            </Button>
          </Space>
          {prepaidFirstPeriod && (
            <Typography.Paragraph type="secondary" style={{ marginTop: 8, marginBottom: 0, fontSize: 12 }}>
              First period ({fmtMoney(onePeriod)}) will be recorded as paid. New interest accrues from period 2.
            </Typography.Paragraph>
          )}
        </>
      }
    />
  );
}
