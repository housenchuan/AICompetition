-- ============================================================
--  AICompetition 核保风险评估系统 —— 数据库建表脚本 (PostgreSQL)
--  表结构依据赛题《业务数据说明（最终版）》，不可修改字段定义。
-- ============================================================

-- 1、历史客户风险画像分析表（所有投保人的基础风险画像信息）
CREATE TABLE IF NOT EXISTS customer_risk_his (
    profile_id               VARCHAR(32)  PRIMARY KEY,
    customer_id              VARCHAR(32)  NOT NULL,
    age                      INT,
    gender                   VARCHAR(10),
    occupation               VARCHAR(100),
    annual_income            DECIMAL(15,2),
    has_social_insurance     BOOLEAN,
    smoking_status           VARCHAR(20),
    drinking_status          VARCHAR(20),
    family_medical_history   TEXT,
    personal_medical_history TEXT,
    bmi                      DECIMAL(5,2),
    blood_pressure           VARCHAR(20),
    created_at               TIMESTAMP,
    updated_at               TIMESTAMP,
    target                   INT,
    score_v1                 INT,
    CONSTRAINT uk_customer_risk_his_customer UNIQUE (customer_id)
);

COMMENT ON TABLE  customer_risk_his                          IS '历史客户风险画像分析表';
COMMENT ON COLUMN customer_risk_his.profile_id               IS '画像唯一标识（主键）';
COMMENT ON COLUMN customer_risk_his.customer_id              IS '投保人编号（唯一）';
COMMENT ON COLUMN customer_risk_his.age                      IS '年龄';
COMMENT ON COLUMN customer_risk_his.gender                   IS '性别 男/女';
COMMENT ON COLUMN customer_risk_his.occupation              IS '职业类别';
COMMENT ON COLUMN customer_risk_his.annual_income            IS '年收入（元）';
COMMENT ON COLUMN customer_risk_his.has_social_insurance     IS '是否有社保';
COMMENT ON COLUMN customer_risk_his.smoking_status           IS '吸烟状况 是/否/已戒烟';
COMMENT ON COLUMN customer_risk_his.drinking_status          IS '饮酒状况 是/否/偶尔';
COMMENT ON COLUMN customer_risk_his.family_medical_history   IS '家族病史';
COMMENT ON COLUMN customer_risk_his.personal_medical_history IS '个人病史';
COMMENT ON COLUMN customer_risk_his.bmi                      IS '身体质量指数';
COMMENT ON COLUMN customer_risk_his.blood_pressure           IS '血压情况';
COMMENT ON COLUMN customer_risk_his.created_at               IS '创建时间';
COMMENT ON COLUMN customer_risk_his.updated_at               IS '更新时间';
COMMENT ON COLUMN customer_risk_his.target                   IS '是否产生理赔 1有理赔/0无理赔';
COMMENT ON COLUMN customer_risk_his.score_v1                 IS '第一版风险评分';

CREATE INDEX IF NOT EXISTS idx_crh_customer   ON customer_risk_his (customer_id);
CREATE INDEX IF NOT EXISTS idx_crh_created_at ON customer_risk_his (created_at);
CREATE INDEX IF NOT EXISTS idx_crh_updated_at ON customer_risk_his (updated_at);

-- 2、投保申请记录表
CREATE TABLE IF NOT EXISTS policy_applications (
    profile_id               VARCHAR(32)  PRIMARY KEY,
    customer_id              VARCHAR(32)  NOT NULL,
    product_type             VARCHAR(50),
    product_name             VARCHAR(100),
    coverage_amount          DECIMAL(15,2),
    premium                  DECIMAL(15,2),
    payment_frequency        VARCHAR(20),
    insurance_period         VARCHAR(20),
    waiting_period           INT,
    beneficiary_relationship VARCHAR(50),
    application_date         DATE,
    status                   VARCHAR(20),
    created_by               VARCHAR(20),
    created_at               TIMESTAMP,
    updated_at               TIMESTAMP
);

COMMENT ON TABLE  policy_applications                          IS '投保申请记录表';
COMMENT ON COLUMN policy_applications.profile_id               IS '画像唯一标识（主键）';
COMMENT ON COLUMN policy_applications.customer_id              IS '投保人编号';
COMMENT ON COLUMN policy_applications.product_type             IS '产品类型 寿险/医疗险/意外险等';
COMMENT ON COLUMN policy_applications.product_name             IS '产品名称';
COMMENT ON COLUMN policy_applications.coverage_amount          IS '保额（元）';
COMMENT ON COLUMN policy_applications.premium                  IS '保费（元）';
COMMENT ON COLUMN policy_applications.payment_frequency        IS '缴费频率 年缴/半年缴/季缴/月缴';
COMMENT ON COLUMN policy_applications.insurance_period         IS '保障期限';
COMMENT ON COLUMN policy_applications.waiting_period           IS '等待期（天数）';
COMMENT ON COLUMN policy_applications.beneficiary_relationship IS '与受益人关系';
COMMENT ON COLUMN policy_applications.application_date         IS '申请日期';
COMMENT ON COLUMN policy_applications.status                   IS '申请状态 待核保/核保中/已通过/已拒保/已撤单';
COMMENT ON COLUMN policy_applications.created_by               IS '创建人 系统/人工';
COMMENT ON COLUMN policy_applications.created_at               IS '创建时间';
COMMENT ON COLUMN policy_applications.updated_at               IS '更新时间';

CREATE INDEX IF NOT EXISTS idx_pa_customer ON policy_applications (customer_id);
CREATE INDEX IF NOT EXISTS idx_pa_app_date ON policy_applications (application_date);
CREATE INDEX IF NOT EXISTS idx_pa_status   ON policy_applications (status);

-- 3、核保决策画像分析结果表（AI 生成字段见备注）
CREATE TABLE IF NOT EXISTS underwriting_decisions (
    decision_id              VARCHAR(32)  PRIMARY KEY,
    application_id           VARCHAR(32),
    customer_id              VARCHAR(32),
    age                      INT,
    gender                   VARCHAR(10),
    occupation               VARCHAR(100),
    annual_income            DECIMAL(15,2),
    has_social_insurance     BOOLEAN,
    smoking_status           VARCHAR(20),
    drinking_status          VARCHAR(20),
    family_medical_history   TEXT,
    personal_medical_history TEXT,
    bmi                      DECIMAL(5,2),
    blood_pressure           VARCHAR(20),
    risk_score               INT,
    risk_level               VARCHAR(20),
    underwriting_result      VARCHAR(50),
    premium_adjustment       DECIMAL(5,2),
    key_factors              TEXT,
    created_by               VARCHAR(20),
    created_at               TIMESTAMP,
    updated_at               TIMESTAMP
);

COMMENT ON TABLE  underwriting_decisions                          IS '核保决策画像分析结果表';
COMMENT ON COLUMN underwriting_decisions.decision_id              IS '核保决策唯一标识（主键）';
COMMENT ON COLUMN underwriting_decisions.application_id           IS '投保申请编号';
COMMENT ON COLUMN underwriting_decisions.customer_id              IS '投保人编号';
COMMENT ON COLUMN underwriting_decisions.age                      IS '年龄';
COMMENT ON COLUMN underwriting_decisions.gender                   IS '性别 男/女';
COMMENT ON COLUMN underwriting_decisions.occupation              IS '职业类别';
COMMENT ON COLUMN underwriting_decisions.annual_income            IS '年收入（元）';
COMMENT ON COLUMN underwriting_decisions.has_social_insurance     IS '是否有社保';
COMMENT ON COLUMN underwriting_decisions.smoking_status           IS '吸烟状况 是/否/已戒烟';
COMMENT ON COLUMN underwriting_decisions.drinking_status          IS '饮酒状况 是/否/偶尔';
COMMENT ON COLUMN underwriting_decisions.family_medical_history   IS '家族病史';
COMMENT ON COLUMN underwriting_decisions.personal_medical_history IS '个人病史';
COMMENT ON COLUMN underwriting_decisions.bmi                      IS '身体质量指数';
COMMENT ON COLUMN underwriting_decisions.blood_pressure           IS '血压情况';
COMMENT ON COLUMN underwriting_decisions.risk_score               IS '风险评分（AI 生成）';
COMMENT ON COLUMN underwriting_decisions.risk_level               IS '风险等级 标准体/次标体/高风险体/拒保（AI 生成）';
COMMENT ON COLUMN underwriting_decisions.underwriting_result      IS '核保结论 标保/加费/除外/延期/拒保（AI 生成）';
COMMENT ON COLUMN underwriting_decisions.premium_adjustment       IS '加费比例 如加费20%记为1.2（AI 生成）';
COMMENT ON COLUMN underwriting_decisions.key_factors              IS '关键风险因子（AI 生成）';
COMMENT ON COLUMN underwriting_decisions.created_by               IS '创建人 系统/人工';
COMMENT ON COLUMN underwriting_decisions.created_at               IS '创建时间';
COMMENT ON COLUMN underwriting_decisions.updated_at               IS '更新时间';

CREATE INDEX IF NOT EXISTS idx_ud_application ON underwriting_decisions (application_id);
CREATE INDEX IF NOT EXISTS idx_ud_customer    ON underwriting_decisions (customer_id);
CREATE INDEX IF NOT EXISTS idx_ud_risk_level  ON underwriting_decisions (risk_level);
