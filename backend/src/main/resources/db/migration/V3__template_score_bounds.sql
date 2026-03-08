-- 做法一：每个模板独立配置理论分数区间（min_score/max_score）
-- 用于展示（如 15/56）、校验、以及新增模板时的规则设计参考

ALTER TABLE assessment_template
  ADD COLUMN min_score DECIMAL(10,2) NULL DEFAULT NULL COMMENT '理论最低分（根据SCORE规则计算）',
  ADD COLUMN max_score DECIMAL(10,2) NULL DEFAULT NULL COMMENT '理论最高分（根据SCORE规则计算）';
