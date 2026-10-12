package com.erp.pa.domain.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class PaOrderDetailVO extends PaOrderListVO {

    private Long deptId;
    private LocalDate deliveryDate;
    private String remark;
    private Long auditBy;
    private LocalDateTime auditTime;
    private String closeReason;
    private List<PaOrderEntryVO> entries;
}








