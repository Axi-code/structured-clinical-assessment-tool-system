package com.medical.assessment.handler;

import com.medical.assessment.util.AesEncryptionUtil;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * MyBatis 加密字符串 TypeHandler
 * 入库时加密，出库时解密，实现 L3 级敏感字段落库加密
 * 通过 @TableField(typeHandler = EncryptedStringTypeHandler.class) 指定使用
 */
public class EncryptedStringTypeHandler extends BaseTypeHandler<String> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, AesEncryptionUtil.encrypt(parameter));
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return value == null ? null : AesEncryptionUtil.decrypt(value);
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return value == null ? null : AesEncryptionUtil.decrypt(value);
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return value == null ? null : AesEncryptionUtil.decrypt(value);
    }
}
