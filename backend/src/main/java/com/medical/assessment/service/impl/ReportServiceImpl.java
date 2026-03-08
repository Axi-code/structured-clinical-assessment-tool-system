package com.medical.assessment.service.impl;

import com.alibaba.fastjson2.JSON;
import com.itextpdf.text.*;
import com.itextpdf.text.Document;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.medical.assessment.entity.AssessmentField;
import com.medical.assessment.entity.AssessmentRecord;
import com.medical.assessment.entity.AssessmentTemplate;
import com.medical.assessment.entity.Patient;
import com.medical.assessment.entity.ReportTemplate;
import com.medical.assessment.util.PatientDesensitizationUtil;
import com.medical.assessment.entity.TreatmentSuggestion;
import com.medical.assessment.service.AssessmentFieldService;
import com.medical.assessment.service.AssessmentRecordService;
import com.medical.assessment.service.AssessmentTemplateService;
import com.medical.assessment.service.DepartmentService;
import com.medical.assessment.service.PatientService;
import com.medical.assessment.service.ReportService;
import com.medical.assessment.service.ReportTemplateService;
import com.medical.assessment.service.TreatmentSuggestionRecordService;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class ReportServiceImpl implements ReportService {
    
    @Autowired
    private AssessmentRecordService recordService;
    
    @Autowired
    private PatientService patientService;
    @Autowired
    private DepartmentService departmentService;
    
    @Autowired
    private AssessmentTemplateService templateService;
    
    @Autowired
    private AssessmentFieldService fieldService;
    
    @Autowired
    private ReportTemplateService reportTemplateService;
    
    @Autowired
    private TreatmentSuggestionRecordService suggestionRecordService;
    
    private static final String[] FONT_PATHS = {
        "C:/Windows/Fonts/simsun.ttc,0",  // Windows 宋体
        "C:/Windows/Fonts/simhei.ttf",     // Windows 黑体
        "/System/Library/Fonts/PingFang.ttc", // macOS
        "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc" // Linux
    };
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_ONLY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String SUGGESTION_SECTION_TITLE = "五、诊疗建议";
    
    /**
     * 获取可用的中文字体
     */
    private BaseFont getChineseFont() throws Exception {
        for (String fontPath : FONT_PATHS) {
            try {
                return BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);
            } catch (Exception e) {
                // 尝试下一个字体路径
                continue;
            }
        }
        // 如果所有字体都加载失败，使用默认字体（可能不支持中文）
        return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
    }
    
    @Override
    public void generatePdfReport(Long recordId, javax.servlet.http.HttpServletResponse response) {
        generatePdfReport(recordId, null, response);
    }
    
    @Override
    public void generatePdfReport(Long recordId, Long templateId, javax.servlet.http.HttpServletResponse response) {
        try {
            AssessmentRecord record = recordService.getById(recordId);
            if (record == null) {
                throw new RuntimeException("评估记录不存在");
            }
            
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=assessment_report_" + record.getRecordNo() + ".pdf");
            
            generatePdfReport(record, templateId, response.getOutputStream());
        } catch (Exception e) {
            throw new RuntimeException("生成PDF报告失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void generateWordReport(Long recordId, javax.servlet.http.HttpServletResponse response) {
        generateWordReport(recordId, null, response);
    }
    
    @Override
    public void generateWordReport(Long recordId, Long templateId, javax.servlet.http.HttpServletResponse response) {
        try {
            AssessmentRecord record = recordService.getById(recordId);
            if (record == null) {
                throw new RuntimeException("评估记录不存在");
            }
            
            response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            response.setHeader("Content-Disposition", "attachment; filename=assessment_report_" + record.getRecordNo() + ".docx");
            
            generateWordReport(record, templateId, response.getOutputStream());
        } catch (Exception e) {
            throw new RuntimeException("生成Word报告失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void generatePdfReport(AssessmentRecord record, OutputStream outputStream) {
        generatePdfReport(record, null, outputStream);
    }
    
    @Override
    public void generatePdfReport(AssessmentRecord record, Long templateId, OutputStream outputStream) {
        try {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, outputStream);
            document.open();
            
            // 设置中文字体
            BaseFont baseFont = getChineseFont();
            Font titleFont = new Font(baseFont, 18, Font.BOLD);
            Font headerFont = new Font(baseFont, 14, Font.BOLD);
            Font normalFont = new Font(baseFont, 12, Font.NORMAL);
            Font smallFont = new Font(baseFont, 10, Font.NORMAL);
            
            // 获取关联数据
            Patient patient = patientService.getById(record.getPatientId());
            patientService.enrichPatient(patient);
            AssessmentTemplate assessmentTemplate = templateService.getById(record.getTemplateId());
            List<AssessmentField> fields = fieldService.getFieldsByTemplateId(record.getTemplateId());
            Map<String, Object> assessmentData = JSON.parseObject(record.getAssessmentData(), Map.class);
            
            // 获取报告模板
            ReportTemplate reportTemplate = getReportTemplate(templateId, record.getTemplateId(), "PDF");
            
            // 应用模板配置
            if (reportTemplate != null && reportTemplate.getTitle() != null && !reportTemplate.getTitle().isEmpty()) {
                Paragraph title = new Paragraph(reportTemplate.getTitle(), titleFont);
                title.setAlignment(Element.ALIGN_CENTER);
                title.setSpacingAfter(20);
                document.add(title);
            } else {
                // 默认标题
                Paragraph title = new Paragraph("智安临评报告", titleFont);
                title.setAlignment(Element.ALIGN_CENTER);
                title.setSpacingAfter(20);
                document.add(title);
            }
            
            // 患者基本信息
            document.add(createSectionHeader("一、患者基本信息", headerFont));
            PdfPTable patientTable = new PdfPTable(4);
            patientTable.setWidthPercentage(100);
            patientTable.setWidths(new float[]{1, 2, 1, 2});
            
            addTableRow(patientTable, "患者编号", patient != null ? patient.getPatientNo() : "", normalFont);
            addTableRow(patientTable, "姓名", patient != null ? PatientDesensitizationUtil.desensitizeName(patient.getName()) : "", normalFont);
            addTableRow(patientTable, "性别", patient != null ? patient.getGender() : "", normalFont);
            addTableRow(patientTable, "年龄", patient != null ? (patient.getAge() != null ? patient.getAge().toString() : "") : "", normalFont);
            addTableRow(patientTable, "科室", patient != null ? patient.getDepartmentName() : "", normalFont);
            addTableRow(patientTable, "诊断", patient != null ? patient.getDiagnosisName() : "", normalFont);
            
            document.add(patientTable);
            document.add(new Paragraph(" ", normalFont));
            
            // 评估记录信息
            document.add(createSectionHeader("二、评估记录信息", headerFont));
            PdfPTable recordTable = new PdfPTable(4);
            recordTable.setWidthPercentage(100);
            recordTable.setWidths(new float[]{1, 2, 1, 2});
            
            addTableRow(recordTable, "记录编号", record.getRecordNo(), normalFont);
            addTableRow(recordTable, "评估模板", assessmentTemplate != null ? assessmentTemplate.getTemplateName() : "", normalFont);
            addTableRow(recordTable, "评估类别", assessmentTemplate != null ? assessmentTemplate.getCategory() : "", normalFont);
            addTableRow(recordTable, "评估时间", record.getCreateTime() != null ? record.getCreateTime().format(DATE_FORMATTER) : "", normalFont);
            addTableRow(recordTable, "评估人", record.getAssessorName(), normalFont);
            String recordDeptName = record.getDepartmentId() != null && departmentService.getById(record.getDepartmentId()) != null
                    ? departmentService.getById(record.getDepartmentId()).getName() : "";
            addTableRow(recordTable, "评估科室", recordDeptName, normalFont);
            addTableRow(recordTable, "状态", record.getStatus() == 1 ? "已完成" : "草稿", normalFont);
            
            document.add(recordTable);
            document.add(new Paragraph(" ", normalFont));
            
            // 评估数据详情
            document.add(createSectionHeader("三、评估数据详情", headerFont));
            PdfPTable dataTable = new PdfPTable(2);
            dataTable.setWidthPercentage(100);
            dataTable.setWidths(new float[]{1, 2});
            
            for (AssessmentField field : fields) {
                Object value = assessmentData != null ? assessmentData.get(field.getFieldCode()) : null;
                String valueStr = value != null ? value.toString() : "";
                addTableRow(dataTable, field.getFieldLabel(), valueStr, normalFont);
            }
            
            document.add(dataTable);
            document.add(new Paragraph(" ", normalFont));
            
            // 评估结果
            document.add(createSectionHeader("四、评估结果", headerFont));
            PdfPTable resultTable = new PdfPTable(4);
            resultTable.setWidthPercentage(100);
            resultTable.setWidths(new float[]{1, 2, 1, 2});
            
            addTableRow(resultTable, "总分", record.getTotalScore() != null ? record.getTotalScore().toString() : "", normalFont);
            addTableRow(resultTable, "评估结果", record.getAssessmentResult() != null ? record.getAssessmentResult() : "", normalFont);
            addTableRow(resultTable, "风险等级", record.getRiskLevel() != null ? record.getRiskLevel() : "", normalFont);
            
            if (record.getRiskTips() != null && !record.getRiskTips().isEmpty()) {
                PdfPCell tipsCell = new PdfPCell(new Phrase("风险提示", headerFont));
                tipsCell.setColspan(1);
                resultTable.addCell(tipsCell);
                
                PdfPCell tipsContentCell = new PdfPCell(new Phrase(record.getRiskTips(), normalFont));
                tipsContentCell.setColspan(3);
                resultTable.addCell(tipsContentCell);
            }
            
            document.add(resultTable);
            document.add(new Paragraph(" ", normalFont));
            
            // 诊疗建议
            TreatmentSuggestion latestSuggestion = getLatestSuggestion(record.getId());
            if (latestSuggestion != null && latestSuggestion.getSuggestionContent() != null && !latestSuggestion.getSuggestionContent().isEmpty()) {
                document.add(createSectionHeader(SUGGESTION_SECTION_TITLE, headerFont));
                Paragraph suggestionPara = new Paragraph(latestSuggestion.getSuggestionContent(), normalFont);
                document.add(suggestionPara);
                document.add(new Paragraph(" ", normalFont));
            }
            
            // 备注
            if (record.getRemark() != null && !record.getRemark().isEmpty()) {
                document.add(createSectionHeader("六、备注", headerFont));
                Paragraph remark = new Paragraph(record.getRemark(), normalFont);
                document.add(remark);
                document.add(new Paragraph(" ", normalFont));
            }
            
            // 报告生成时间
            Paragraph footer = new Paragraph("报告生成时间: " + LocalDateTime.now().format(DATE_FORMATTER), smallFont);
            footer.setAlignment(Element.ALIGN_RIGHT);
            document.add(footer);
            
            document.close();
        } catch (Exception e) {
            throw new RuntimeException("生成PDF报告失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void generateWordReport(AssessmentRecord record, OutputStream outputStream) {
        generateWordReport(record, null, outputStream);
    }
    
    @Override
    public void generateWordReport(AssessmentRecord record, Long templateId, OutputStream outputStream) {
        try {
            XWPFDocument document = new XWPFDocument();
            
            // 获取关联数据
            Patient patient = patientService.getById(record.getPatientId());
            patientService.enrichPatient(patient);
            AssessmentTemplate assessmentTemplate = templateService.getById(record.getTemplateId());
            List<AssessmentField> fields = fieldService.getFieldsByTemplateId(record.getTemplateId());
            Map<String, Object> assessmentData = JSON.parseObject(record.getAssessmentData(), Map.class);
            
            // 获取报告模板
            ReportTemplate reportTemplate = getReportTemplate(templateId, record.getTemplateId(), "WORD");
            
            // 标题
            XWPFParagraph titlePara = document.createParagraph();
            titlePara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titlePara.createRun();
            if (reportTemplate != null && reportTemplate.getTitle() != null && !reportTemplate.getTitle().isEmpty()) {
                titleRun.setText(reportTemplate.getTitle());
            } else {
                titleRun.setText("智安临评报告");
            }
            titleRun.setBold(true);
            titleRun.setFontSize(18);
            titleRun.setFontFamily("宋体");
            
            // 患者基本信息
            addWordSectionHeader(document, "一、患者基本信息");
            XWPFTable patientTable = document.createTable(6, 4);
            patientTable.setWidth("100%");
            
            setWordTableRow(patientTable, 0, "患者编号", patient != null ? patient.getPatientNo() : "");
            setWordTableRow(patientTable, 1, "姓名", patient != null ? PatientDesensitizationUtil.desensitizeName(patient.getName()) : "");
            setWordTableRow(patientTable, 2, "性别", patient != null ? patient.getGender() : "");
            setWordTableRow(patientTable, 3, "年龄", patient != null ? (patient.getAge() != null ? patient.getAge().toString() : "") : "");
            setWordTableRow(patientTable, 4, "科室", patient != null ? patient.getDepartmentName() : "");
            setWordTableRow(patientTable, 5, "诊断", patient != null ? patient.getDiagnosisName() : "");
            
            // 评估记录信息
            addWordSectionHeader(document, "二、评估记录信息");
            XWPFTable recordTable = document.createTable(7, 4);
            recordTable.setWidth("100%");
            
            setWordTableRow(recordTable, 0, "记录编号", record.getRecordNo());
            setWordTableRow(recordTable, 1, "评估模板", assessmentTemplate != null ? assessmentTemplate.getTemplateName() : "");
            setWordTableRow(recordTable, 2, "评估类别", assessmentTemplate != null ? assessmentTemplate.getCategory() : "");
            setWordTableRow(recordTable, 3, "评估时间", record.getCreateTime() != null ? record.getCreateTime().format(DATE_FORMATTER) : "");
            setWordTableRow(recordTable, 4, "评估人", record.getAssessorName());
            String wordRecordDept = record.getDepartmentId() != null && departmentService.getById(record.getDepartmentId()) != null
                    ? departmentService.getById(record.getDepartmentId()).getName() : "";
            setWordTableRow(recordTable, 5, "评估科室", wordRecordDept);
            setWordTableRow(recordTable, 6, "状态", record.getStatus() == 1 ? "已完成" : "草稿");
            
            // 评估数据详情
            addWordSectionHeader(document, "三、评估数据详情");
            XWPFTable dataTable = document.createTable(fields.size(), 2);
            dataTable.setWidth("100%");
            
            for (int i = 0; i < fields.size(); i++) {
                AssessmentField field = fields.get(i);
                Object value = assessmentData != null ? assessmentData.get(field.getFieldCode()) : null;
                String valueStr = value != null ? value.toString() : "";
                setWordTableRow(dataTable, i, field.getFieldLabel(), valueStr);
            }
            
            // 评估结果
            addWordSectionHeader(document, "四、评估结果");
            XWPFTable resultTable = document.createTable(4, 4);
            resultTable.setWidth("100%");
            
            setWordTableRow(resultTable, 0, "总分", record.getTotalScore() != null ? record.getTotalScore().toString() : "");
            setWordTableRow(resultTable, 1, "评估结果", record.getAssessmentResult() != null ? record.getAssessmentResult() : "");
            setWordTableRow(resultTable, 2, "风险等级", record.getRiskLevel() != null ? record.getRiskLevel() : "");
            
            if (record.getRiskTips() != null && !record.getRiskTips().isEmpty()) {
                XWPFTableRow tipsRow = resultTable.getRow(3);
                tipsRow.getCell(0).setText("风险提示");
                tipsRow.getCell(1).setText(record.getRiskTips());
                tipsRow.getCell(2).setText("");
                tipsRow.getCell(3).setText("");
            }
            
            // 诊疗建议
            TreatmentSuggestion latestSuggestion = getLatestSuggestion(record.getId());
            if (latestSuggestion != null && latestSuggestion.getSuggestionContent() != null && !latestSuggestion.getSuggestionContent().isEmpty()) {
                addWordSectionHeader(document, SUGGESTION_SECTION_TITLE);
                XWPFParagraph suggestionPara = document.createParagraph();
                suggestionPara.setAlignment(ParagraphAlignment.LEFT);
                suggestionPara.setSpacingBetween(1.3);
                suggestionPara.setSpacingBefore(120);
                suggestionPara.setIndentationFirstLine(420); // 首行缩进约0.75字符
                XWPFRun suggestionRun = suggestionPara.createRun();
                suggestionRun.setFontFamily("宋体");
                suggestionRun.setFontSize(12);
                String[] lines = latestSuggestion.getSuggestionContent().split("\\r?\\n");
                boolean firstLine = true;
                for (String line : lines) {
                    if (!firstLine) {
                        suggestionRun.addBreak();
                    }
                    if (line.isEmpty()) {
                        // 保留空行
                        suggestionRun.addBreak();
                    } else {
                        suggestionRun.setText(line);
                    }
                    firstLine = false;
                }
            }
            
            // 备注
            if (record.getRemark() != null && !record.getRemark().isEmpty()) {
                addWordSectionHeader(document, "六、备注");
                XWPFParagraph remarkPara = document.createParagraph();
                XWPFRun remarkRun = remarkPara.createRun();
                remarkRun.setText(record.getRemark());
            }
            
            // 报告生成时间
            XWPFParagraph footerPara = document.createParagraph();
            footerPara.setAlignment(ParagraphAlignment.RIGHT);
            XWPFRun footerRun = footerPara.createRun();
            footerRun.setText("报告生成时间: " + LocalDateTime.now().format(DATE_FORMATTER));
            footerRun.setFontSize(10);
            
            document.write(outputStream);
            document.close();
        } catch (Exception e) {
            throw new RuntimeException("生成Word报告失败: " + e.getMessage(), e);
        }
    }
    
    private Paragraph createSectionHeader(String text, Font font) {
        Paragraph para = new Paragraph(text, font);
        para.setSpacingBefore(10);
        para.setSpacingAfter(10);
        return para;
    }
    
    private void addTableRow(PdfPTable table, String label, String value, Font font) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, font));
        labelCell.setPadding(8);
        labelCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        table.addCell(labelCell);
        
        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "", font));
        valueCell.setPadding(8);
        table.addCell(valueCell);
    }
    
    private void addWordSectionHeader(XWPFDocument document, String text) {
        XWPFParagraph para = document.createParagraph();
        XWPFRun run = para.createRun();
        run.setText(text);
        run.setBold(true);
        run.setFontSize(14);
        run.setFontFamily("宋体");
    }
    
    private void setWordTableRow(XWPFTable table, int rowIndex, String label, String value) {
        XWPFTableRow row = table.getRow(rowIndex);
        row.getCell(0).setText(label);
        row.getCell(1).setText(value != null ? value : "");
        if (row.getTableCells().size() > 2) {
            row.getCell(2).setText("");
            row.getCell(3).setText("");
        }
    }
    
    /**
     * 获取最新的诊疗建议（按创建时间倒序）
     */
    private TreatmentSuggestion getLatestSuggestion(Long recordId) {
        if (recordId == null) {
            return null;
        }
        List<TreatmentSuggestion> suggestions = suggestionRecordService.getByAssessmentRecordId(recordId);
        if (suggestions == null || suggestions.isEmpty()) {
            return null;
        }
        return suggestions.get(0);
    }
    
    /**
     * 获取报告模板（如果templateId为null，则获取默认模板）
     */
    private ReportTemplate getReportTemplate(Long templateId, Long assessmentTemplateId, String reportType) {
        if (templateId != null) {
            ReportTemplate template = reportTemplateService.getById(templateId);
            if (template != null && template.getDeleted() == 0 && template.getStatus() == 1) {
                return template;
            }
        }
        // 获取默认模板
        return reportTemplateService.getDefaultTemplate(assessmentTemplateId, reportType);
    }
    
    
    @Override
    public String previewPdfReport(Long recordId, Long templateId) {
        try {
            AssessmentRecord record = recordService.getById(recordId);
            if (record == null) {
                throw new RuntimeException("评估记录不存在");
            }
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            generatePdfReport(record, templateId, baos);
            byte[] pdfBytes = baos.toByteArray();
            return Base64.getEncoder().encodeToString(pdfBytes);
        } catch (Exception e) {
            throw new RuntimeException("预览PDF报告失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String previewWordReport(Long recordId, Long templateId) {
        try {
            AssessmentRecord record = recordService.getById(recordId);
            if (record == null) {
                throw new RuntimeException("评估记录不存在");
            }
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            generateWordReport(record, templateId, baos);
            byte[] wordBytes = baos.toByteArray();
            return Base64.getEncoder().encodeToString(wordBytes);
        } catch (Exception e) {
            throw new RuntimeException("预览Word报告失败: " + e.getMessage(), e);
        }
    }
}
