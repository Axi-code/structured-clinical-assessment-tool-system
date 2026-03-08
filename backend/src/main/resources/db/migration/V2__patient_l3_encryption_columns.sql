-- L3 级患者隐私保护：扩展敏感字段列宽以存储 AES 加密后的 Base64 密文
-- 加密后长度约为原长度的 1.5 倍（Base64 膨胀），需预留足够空间

ALTER TABLE `patient`
  MODIFY COLUMN `id_card` varchar(100) NULL DEFAULT NULL COMMENT '身份证号（加密存储）',
  MODIFY COLUMN `phone` varchar(100) NULL DEFAULT NULL COMMENT '联系电话（加密存储）',
  MODIFY COLUMN `address` varchar(500) NULL DEFAULT NULL COMMENT '地址（加密存储）',
  MODIFY COLUMN `emergency_contact` varchar(150) NULL DEFAULT NULL COMMENT '紧急联系人（加密存储）',
  MODIFY COLUMN `emergency_phone` varchar(100) NULL DEFAULT NULL COMMENT '紧急联系电话（加密存储）';
