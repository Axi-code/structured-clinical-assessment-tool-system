ALTER TABLE assessment_record
  ADD COLUMN ai_diagnosis_name varchar(100) NULL DEFAULT NULL COMMENT 'AI建议诊断名称';
