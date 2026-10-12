package com.erp.pm.domain.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PmPickVO {

    private Long id;
    private String pickNo;
    private Long moId;
    private String moNo;
    private Long warehouseId;
    private String warehouseName;
    private LocalDate pickDate;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private List<PmPickEntryVO> entries;
}








