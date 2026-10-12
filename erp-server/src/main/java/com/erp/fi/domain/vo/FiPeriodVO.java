package com.erp.fi.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FiPeriodVO {

    private Long id;
    private Integer periodYear;
    private Integer periodNo;
    private String periodMonth;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
}








