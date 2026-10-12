package com.erp.ic.domain.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class IcTransferListVO {

    private Long id;
    private String transferNo;
    private Long fromWarehouseId;
    private String fromWarehouseName;
    private Long toWarehouseId;
    private String toWarehouseName;
    private LocalDate transferDate;
    private Integer status;
    private LocalDateTime createTime;
}








