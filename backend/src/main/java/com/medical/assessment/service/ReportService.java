package com.medical.assessment.service;

import com.medical.assessment.entity.AssessmentRecord;

import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;

/**
 * 评估报告生成服务
 */
public interface ReportService {
    
    /**
     * 生成PDF报告并写入响应流
     * @param recordId 评估记录ID
     * @param response HTTP响应对象
     */
    void generatePdfReport(Long recordId, HttpServletResponse response);
    
    /**
     * 生成PDF报告并写入响应流（使用自定义模板）
     * @param recordId 评估记录ID
     * @param templateId 报告模板ID（可选，为null时使用默认模板）
     * @param response HTTP响应对象
     */
    void generatePdfReport(Long recordId, Long templateId, HttpServletResponse response);
    
    /**
     * 生成Word报告并写入响应流
     * @param recordId 评估记录ID
     * @param response HTTP响应对象
     */
    void generateWordReport(Long recordId, HttpServletResponse response);
    
    /**
     * 生成Word报告并写入响应流（使用自定义模板）
     * @param recordId 评估记录ID
     * @param templateId 报告模板ID（可选，为null时使用默认模板）
     * @param response HTTP响应对象
     */
    void generateWordReport(Long recordId, Long templateId, HttpServletResponse response);
    
    /**
     * 生成PDF报告到输出流
     * @param record 评估记录
     * @param outputStream 输出流
     */
    void generatePdfReport(AssessmentRecord record, OutputStream outputStream);
    
    /**
     * 生成PDF报告到输出流（使用自定义模板）
     * @param record 评估记录
     * @param templateId 报告模板ID（可选，为null时使用默认模板）
     * @param outputStream 输出流
     */
    void generatePdfReport(AssessmentRecord record, Long templateId, OutputStream outputStream);
    
    /**
     * 生成Word报告到输出流
     * @param record 评估记录
     * @param outputStream 输出流
     */
    void generateWordReport(AssessmentRecord record, OutputStream outputStream);
    
    /**
     * 生成Word报告到输出流（使用自定义模板）
     * @param record 评估记录
     * @param templateId 报告模板ID（可选，为null时使用默认模板）
     * @param outputStream 输出流
     */
    void generateWordReport(AssessmentRecord record, Long templateId, OutputStream outputStream);
    
    /**
     * 生成PDF报告并返回Base64编码（用于预览）
     * @param recordId 评估记录ID
     * @param templateId 报告模板ID（可选，为null时使用默认模板）
     * @return Base64编码的PDF内容
     */
    String previewPdfReport(Long recordId, Long templateId);
    
    /**
     * 生成Word报告并返回Base64编码（用于预览）
     * @param recordId 评估记录ID
     * @param templateId 报告模板ID（可选，为null时使用默认模板）
     * @return Base64编码的Word内容
     */
    String previewWordReport(Long recordId, Long templateId);
}
