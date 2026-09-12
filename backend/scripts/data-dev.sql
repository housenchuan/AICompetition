-- 开发联调用样例数据（非赛题 demo 数据），仅用于本地验证接口与规则引擎。
-- 用法：psql -U housenchuan -d aicompetition -f backend/scripts/data-dev.sql

TRUNCATE customer_risk_his, policy_applications, underwriting_decisions;

INSERT INTO customer_risk_his
(profile_id, customer_id, age, gender, occupation, annual_income, has_social_insurance,
 smoking_status, drinking_status, family_medical_history, personal_medical_history,
 bmi, blood_pressure, created_at, updated_at, target, score_v1)
VALUES
('P001','C001',30,'男','办公室职员',180000,true,'不吸烟','不饮酒','无','无',22.0,'118/76','2025-06-03 10:00:00','2025-06-03 10:00:00',0,0),
('P002','C002',55,'男','司机',120000,true,'吸烟','经常饮酒','父亲高血压','2型糖尿病',29.0,'145/92','2025-06-10 09:30:00','2025-06-10 09:30:00',1,80),
('P003','C003',62,'女','退休',90000,true,'不吸烟','偶尔','无','恶性肿瘤病史',24.0,'130/85','2025-05-20 14:00:00','2025-05-20 14:00:00',1,0);

INSERT INTO policy_applications
(application_id, customer_id, product_type, product_name, coverage_amount, premium,
 payment_frequency, insurance_period, waiting_period, beneficiary_relationship,
 application_date, status, created_by, created_at, updated_at)
VALUES
('A001','C001','寿险','安心终身寿',1000000,3200,'年缴','终身',180,'配偶','2025-06-05','已通过','系统','2025-06-05 10:05:00','2025-06-05 10:05:00'),
('A002','C002','医疗险','百万医疗',3000000,4800,'年缴','1年',30,'子女','2025-06-12','核保中','人工','2025-06-12 09:40:00','2025-06-12 09:40:00');

INSERT INTO underwriting_decisions
(decision_id, application_id, customer_id, age, gender, occupation, annual_income, has_social_insurance,
 smoking_status, drinking_status, family_medical_history, personal_medical_history, bmi, blood_pressure,
 risk_score, risk_level, underwriting_result, premium_adjustment, key_factors, created_by, created_at, updated_at)
VALUES
('D001','A001','C001',30,'男','办公室职员',180000,true,'不吸烟','不饮酒','无','无',22.0,'118/76',
 NULL,NULL,NULL,NULL,NULL,'系统','2025-06-05 10:06:00','2025-06-05 10:06:00'),
('D002','A002','C002',55,'男','司机',120000,true,'吸烟','经常饮酒','父亲高血压','2型糖尿病',29.0,'145/92',
 NULL,NULL,NULL,NULL,NULL,'系统','2025-06-12 09:41:00','2025-06-12 09:41:00');
