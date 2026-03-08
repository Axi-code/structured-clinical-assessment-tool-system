package com.medical.assessment.service;

import java.util.Map;

/**
 * Qwen API服务接口
 */
public interface QwenService {
    /**
     * 调用Qwen模型生成文本
     * @param prompt 提示词
     * @return 生成的文本
     */
    String generateText(String prompt);
    
    /**
     * 调用Qwen模型生成文本（带参数）
     * @param prompt 提示词
     * @param parameters 额外参数
     * @return 生成的文本
     */
    String generateText(String prompt, Map<String, Object> parameters);
}
