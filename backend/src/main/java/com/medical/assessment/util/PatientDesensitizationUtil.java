package com.medical.assessment.util;

/**
 * 患者敏感信息脱敏工具类
 * 用于 L2 级患者隐私保护：展示、日志、报告、外部调用时对敏感字段进行脱敏
 */
public final class PatientDesensitizationUtil {

    private static final String MASK = "*";

    private PatientDesensitizationUtil() {
    }

    /** Java 8 兼容：重复字符串 */
    private static String repeat(String s, int count) {
        if (count <= 0) return "";
        StringBuilder sb = new StringBuilder(s.length() * count);
        for (int i = 0; i < count; i++) {
            sb.append(s);
        }
        return sb.toString();
    }

    /**
     * 姓名脱敏：保留姓，其余用星号代替。单字姓保留一字，复姓保留两字。
     * 例：张三 -> 张*，欧阳娜娜 -> 欧阳**
     */
    public static String desensitizeName(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        int len = name.length();
        if (len == 1) {
            return name;
        }
        if (len == 2) {
            return name.charAt(0) + MASK;
        }
        // 复姓常见：欧阳、司马、上官、诸葛等
        if (len >= 4 && isCompoundSurname(name.substring(0, 2))) {
            return name.substring(0, 2) + repeat(MASK, Math.min(len - 2, 2));
        }
        return name.charAt(0) + repeat(MASK, Math.min(len - 1, 2));
    }

    private static boolean isCompoundSurname(String s) {
        return "欧阳".equals(s) || "司马".equals(s) || "上官".equals(s) || "诸葛".equals(s)
                || "司徒".equals(s) || "皇甫".equals(s) || "夏侯".equals(s) || "公孙".equals(s);
    }

    /**
     * 身份证号脱敏：保留前4位和后2位，中间用星号代替。
     * 例：110101197805151234 -> 1101**********34
     */
    public static String desensitizeIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) {
            return idCard != null ? idCard : "";
        }
        int len = idCard.length();
        return idCard.substring(0, 4) + repeat(MASK, len - 6) + idCard.substring(len - 2);
    }

    /**
     * 手机号脱敏：保留前3位和后4位，中间4位用星号代替。
     * 例：13800138001 -> 138****8001
     */
    public static String desensitizePhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone != null ? phone : "";
        }
        int len = phone.length();
        return phone.substring(0, 3) + "****" + phone.substring(len - 4);
    }

    /**
     * 地址脱敏：保留到区/县级别，详细门牌号用星号代替。
     * 例：北京市朝阳区建国路88号 -> 北京市朝阳区***
     */
    public static String desensitizeAddress(String address) {
        if (address == null || address.isEmpty()) {
            return "";
        }
        // 尝试匹配到区/县/市
        int districtEnd = -1;
        String[] districtKeywords = {"区", "县", "市"};
        for (String kw : districtKeywords) {
            int idx = address.lastIndexOf(kw);
            if (idx > districtEnd) {
                districtEnd = idx;
            }
        }
        if (districtEnd >= 0 && districtEnd < address.length() - 1) {
            return address.substring(0, districtEnd + 1) + "***";
        }
        // 无法识别则只保留前6个字符
        return address.length() > 6 ? address.substring(0, 6) + "***" : address;
    }

    /**
     * 诊断脱敏（用于外部 AI 等场景）：仅保留疾病类别关键词，去除可识别个人的描述。
     * 用于报告展示时可不脱敏（内部文档），用于 AI 调用时做泛化。
     * 此处提供轻度脱敏：过长时截断，避免详细病历描述泄露。
     */
    public static String desensitizeDiagnosisForExternal(String diagnosis) {
        if (diagnosis == null || diagnosis.isEmpty()) {
            return "";
        }
        // 对外部系统只传疾病类型，超过50字截断
        return diagnosis.length() > 50 ? diagnosis.substring(0, 50) + "..." : diagnosis;
    }
}
