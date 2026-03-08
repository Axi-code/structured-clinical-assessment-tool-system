-- 评估模板版本控制：允许同一 template_code 存在多个版本（不同 version）
-- 将 template_code 唯一约束改为 (template_code, version) 联合唯一，以支持“基于原版本创建新版本”
-- 执行前若表已有同名索引请先 DROP INDEX template_code

ALTER TABLE `assessment_template` DROP INDEX `template_code`;
ALTER TABLE `assessment_template` ADD UNIQUE KEY `uk_template_code_version` (`template_code`, `version`);
