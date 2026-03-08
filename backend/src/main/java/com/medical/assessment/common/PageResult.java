package com.medical.assessment.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class PageResult<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Long total;
    private List<T> records;
    
    public PageResult() {}
    
    public PageResult(Long total, List<T> records) {
        this.total = total;
        this.records = records;
    }

    public PageResult(Page<T> page) {
        this.total = page.getTotal();
        this.records = page.getRecords();
    }
    
    public static <T> PageResult<T> of(Page<T> page) {
        return new PageResult<>(page.getTotal(), page.getRecords());
    }
}

