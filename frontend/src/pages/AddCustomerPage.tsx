import { UploadOutlined } from '@ant-design/icons';

import {

  Button, Card, Col, Form, Input, InputNumber, Row, Select, Upload, message,

} from 'antd';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { useState } from 'react';

import { useNavigate } from 'react-router-dom';

import { customerApi, masterApi } from '../api/client';

import { InterestCalcPanel } from '../components/InterestCalcPanel';

import { todayStr } from '../utils/format';



export default function AddCustomerPage() {

  const [form] = Form.useForm();

  const navigate = useNavigate();

  const qc = useQueryClient();

  const [idProof, setIdProof] = useState<File | null>(null);

  const [materialPhoto, setMaterialPhoto] = useState<File | null>(null);

  const [prepaidFirstPeriod, setPrepaidFirstPeriod] = useState(false);



  const pledgeAmount = Form.useWatch('pledgeAmount', form);

  const pledgeDate = Form.useWatch('pledgeDate', form);

  const interestRate = Form.useWatch('interestRate', form);

  const interestType = Form.useWatch('interestType', form) ?? 'monthly';



  const { data: settings } = useQuery({ queryKey: ['settings'], queryFn: masterApi.settings });



  const create = useMutation({

    mutationFn: customerApi.create,

    onSuccess: (c) => {

      message.success('Customer created');

      qc.invalidateQueries({ queryKey: ['customers'] });

      navigate(`/customers/${c.id}`);

    },

    onError: (e) => message.error(String(e)),

  });



  const onFinish = (v: Record<string, string | number>) => {

    const fd = new FormData();

    Object.entries(v).forEach(([k, val]) => {

      if (val !== undefined && val !== null) fd.append(k, String(val));

    });

    fd.append('prepaidFirstPeriod', String(prepaidFirstPeriod));

    if (idProof) fd.append('idProofFile', idProof);

    if (materialPhoto) fd.append('materialPhoto', materialPhoto);

    create.mutate(fd);

  };



  return (

    <Card title="Add Customer & First Pledge">

      <Form form={form} layout="vertical" onFinish={onFinish}

        initialValues={{ pledgeDate: todayStr(), interestType: 'monthly', interestRate: 2 }}>

        <Row gutter={16}>

          <Col xs={24} md={8}>

            <Form.Item name="name" label="Customer Name" rules={[{ required: true }]}>

              <Input placeholder="Full name" />

            </Form.Item>

          </Col>

          <Col xs={24} md={8}>

            <Form.Item name="phone" label="Phone" rules={[{ required: true }]}>

              <Input placeholder="10-digit mobile" />

            </Form.Item>

          </Col>

          <Col xs={24} md={8}>

            <Form.Item name="idProof" label="ID Proof (text)">

              <Input placeholder="Aadhaar / PAN / Voter ID" />

            </Form.Item>

          </Col>

        </Row>

        <Row gutter={16}>

          <Col xs={24} md={12}>

            <Form.Item name="address" label="Address"><Input.TextArea rows={2} /></Form.Item>

          </Col>

          <Col xs={24} md={12}>

            <Form.Item label="Upload ID Proof Document">

              <Upload beforeUpload={(f) => { setIdProof(f); return false; }} maxCount={1} accept="image/*,.pdf">

                <Button icon={<UploadOutlined />}>Select ID File</Button>

              </Upload>

            </Form.Item>

          </Col>

        </Row>



        <Card type="inner" title="First Pledge Details" style={{ marginBottom: 16 }}>

          <Row gutter={16}>

            <Col xs={24} md={6}>

              <Form.Item name="material" label="Material" rules={[{ required: true }]}>

                <Select options={(settings?.materials ?? []).map((m) => ({ value: m, label: m }))} placeholder="Select" />

              </Form.Item>

            </Col>

            <Col xs={24} md={6}>

              <Form.Item name="item" label="Item" rules={[{ required: true }]}>

                <Input placeholder="e.g. 22K Necklace" />

              </Form.Item>

            </Col>

            <Col xs={24} md={4}>

              <Form.Item name="grossWeight" label="Gross (g)" rules={[{ required: true }]}>

                <InputNumber min={0} style={{ width: '100%' }} />

              </Form.Item>

            </Col>

            <Col xs={24} md={4}>

              <Form.Item name="netWeight" label="Net (g)" rules={[{ required: true }]}>

                <InputNumber min={0} style={{ width: '100%' }} />

              </Form.Item>

            </Col>

            <Col xs={24} md={4}>

              <Form.Item name="purity" label="Purity" rules={[{ required: true }]}>

                <Input placeholder="22K" />

              </Form.Item>

            </Col>

          </Row>

          <Row gutter={16}>

            <Col xs={24} md={6}>

              <Form.Item name="pledgeAmount" label="Pledge Amount (₹)" rules={[{ required: true }]}>

                <InputNumber min={0} style={{ width: '100%' }} />

              </Form.Item>

            </Col>

            <Col xs={24} md={6}>

              <Form.Item name="pledgeDate" label="Pledge Date" rules={[{ required: true }]}>

                <Input type="date" />

              </Form.Item>

            </Col>

            <Col xs={24} md={6}>

              <Form.Item name="interestRate" label="Interest Rate (%)" rules={[{ required: true }]}>

                <InputNumber min={0} step={0.1} style={{ width: '100%' }} />

              </Form.Item>

            </Col>

            <Col xs={24} md={6}>

              <Form.Item name="interestType" label="Interest Type">

                <Select options={[{ value: 'monthly', label: 'Monthly' }, { value: 'daily', label: 'Daily' }]} />

              </Form.Item>

            </Col>

          </Row>



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



          <Form.Item label="Upload Material Photo">

            <Upload beforeUpload={(f) => { setMaterialPhoto(f); return false; }} maxCount={1} accept="image/*,.pdf">

              <Button icon={<UploadOutlined />}>Select Material Photo</Button>

            </Upload>

          </Form.Item>

        </Card>



        <Button type="primary" htmlType="submit" loading={create.isPending} size="large">

          Create Customer

        </Button>

      </Form>

    </Card>

  );

}

