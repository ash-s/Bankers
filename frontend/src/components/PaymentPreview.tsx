import { Alert, Col, Row, Statistic } from 'antd';
import { fmtMoney } from '../utils/format';

interface PaymentPreviewProps {
  type: 'principal' | 'interest' | 'discount' | string;
  amount?: number;
  outstandingPrincipal?: number;
  interestDue?: number;
  periodInterest?: number;
  periodLabel?: string;
  label?: string;
  compact?: boolean;
}

export function PaymentPreview({
  type, amount, outstandingPrincipal, interestDue, periodInterest,
  periodLabel = '1 Calendar Month Interest', label, compact,
}: PaymentPreviewProps) {
  const amt = amount ?? 0;

  let afterPrincipal = outstandingPrincipal;
  let afterInterest = interestDue;

  if (type === 'principal' && outstandingPrincipal != null && amt > 0) {
    afterPrincipal = Math.max(0, outstandingPrincipal - amt);
  }
  if ((type === 'interest' || type === 'discount') && interestDue != null && amt > 0) {
    afterInterest = Math.max(0, interestDue - amt);
  }

  const statStyle = compact
    ? { fontSize: 13, lineHeight: 1.2 }
    : { fontSize: 16 };
  const titleStyle = compact ? { fontSize: 11, marginBottom: 2 } : undefined;
  const spacing = compact ? 8 : 16;

  return (
    <>
      {(outstandingPrincipal != null || interestDue != null || periodInterest != null) && (
        <Row gutter={compact ? 8 : 16} style={{ marginBottom: spacing }}>
          {outstandingPrincipal != null && (
            <Col xs={24} sm={periodInterest != null ? 8 : 12}>
              <Statistic
                title={<span style={titleStyle}>Principal Outstanding</span>}
                value={fmtMoney(outstandingPrincipal)}
                valueStyle={statStyle}
              />
            </Col>
          )}
          {interestDue != null && (
            <Col xs={24} sm={periodInterest != null ? 8 : 12}>
              <Statistic
                title={<span style={titleStyle}>Interest Due</span>}
                value={fmtMoney(interestDue)}
                valueStyle={{ ...statStyle, color: '#cf1322' }}
              />
            </Col>
          )}
          {periodInterest != null && (
            <Col xs={24} sm={8}>
              <Statistic
                title={<span style={titleStyle}>{periodLabel}</span>}
                value={fmtMoney(periodInterest)}
                valueStyle={{ ...statStyle, color: '#1677ff' }}
              />
            </Col>
          )}
        </Row>
      )}
      {amt > 0 && (
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: spacing, padding: compact ? '6px 10px' : undefined }}
          message={
            <span style={compact ? { fontSize: 12 } : undefined}>
              {label
                ? label
                : `Payment preview: ${fmtMoney(amt)} will be recorded as ${type} payment`}
            </span>
          }
          description={
            type === 'discount'
              ? `Interest balance after discount: ${fmtMoney(afterInterest ?? 0)}`
              : afterPrincipal != null && type === 'principal'
                ? `Principal after payment: ${fmtMoney(afterPrincipal)}`
                : afterInterest != null && type === 'interest'
                  ? `Interest balance after payment: ${fmtMoney(afterInterest)}`
                  : undefined
          }
        />
      )}
    </>
  );
}
