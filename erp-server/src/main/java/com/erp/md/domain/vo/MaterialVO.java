package com.erp.md.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MaterialVO {

    private Long id;
    private String materialCode;
    private String materialName;
    private String spec;
    private Long categoryId;
    private String unit;
    private Integer trackBatch;
    private Integer trackSn;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
}








