package com.medical.assessment.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.medical.assessment.config.QwenConfig;
import com.medical.assessment.service.QwenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Qwen API服务实现
 */
@Service
public class QwenServiceImpl implements QwenService {
    
    @Autowired
    private QwenConfig qwenConfig;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Override
    public String generateText(String prompt) {
        return generateText(prompt, null);
    }
    
    @Override
    public String generateText(String prompt, Map<String, Object> parameters) {
        try {
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", qwenConfig.getModel());
            
            // 构建消息
            Map<String, String> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);
            
            Map<String, Object> input = new HashMap<>();
            input.put("messages", new Map[]{message});
            requestBody.put("input", input);
            
            // 添加参数
            Map<String, Object> parametersMap = new HashMap<>();
            // 设置默认参数
            parametersMap.put("temperature", 0.7);
            parametersMap.put("max_tokens", 2000);
            if (parameters != null) {
                parametersMap.putAll(parameters);
            }
            requestBody.put("parameters", parametersMap);
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + qwenConfig.getApiKey());
            headers.set("X-DashScope-SSE", "disable");
            
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            
            // 发送请求
            String apiUrl = qwenConfig.getApiUrl();
            if (apiUrl == null || apiUrl.trim().isEmpty()) {
                throw new RuntimeException("Qwen API地址未配置");
            }
            ResponseEntity<String> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                requestEntity,
                String.class
            );
            
            // 解析响应
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JSONObject responseJson = JSON.parseObject(response.getBody());
                
                // 检查是否有错误
                if (responseJson.containsKey("code") && !"Success".equals(responseJson.getString("code"))) {
                    String errorMessage = responseJson.getString("message");
                    throw new RuntimeException("Qwen API返回错误: " + errorMessage);
                }
                
                // 解析output
                JSONObject output = responseJson.getJSONObject("output");
                if (output != null) {
                    // 尝试从text字段获取
                    String text = output.getString("text");
                    if (text != null && !text.isEmpty()) {
                        return text;
                    }
                    
                    // 尝试从choices数组获取
                    Object choicesObj = output.get("choices");
                    if (choicesObj != null) {
                        if (choicesObj instanceof List) {
                            List<?> choices = (List<?>) choicesObj;
                            if (!choices.isEmpty()) {
                                Object firstChoice = choices.get(0);
                                if (firstChoice instanceof Map) {
                                    Map<?, ?> choiceMap = (Map<?, ?>) firstChoice;
                                    Object messageObj = choiceMap.get("message");
                                    if (messageObj instanceof Map) {
                                        Map<?, ?> messageMap = (Map<?, ?>) messageObj;
                                        Object content = messageMap.get("content");
                                        if (content != null) {
                                            return content.toString();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            throw new RuntimeException("Qwen API返回异常: " + response.getBody());
        } catch (Exception e) {
            throw new RuntimeException("调用Qwen API失败: " + e.getMessage(), e);
        }
    }
}
