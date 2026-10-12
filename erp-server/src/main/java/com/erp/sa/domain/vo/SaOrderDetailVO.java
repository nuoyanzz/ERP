package com.erp.sa.domain.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class SaOrderDetailVO extends SaOrderListVO {

    private Long deptId;
    private LocalDate deliveryDate;
    private String remark;
    private Long auditBy;
    private LocalDateTime auditTime;
    private String closeReason;
    private List<SaOrderEntryVO> entries;
}








