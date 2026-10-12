package com.erp.md.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WarehouseVO {

    private Long id;
    private String warehouseCode;
    private String warehouseName;
    private Integer whType;
    private String location;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
}








