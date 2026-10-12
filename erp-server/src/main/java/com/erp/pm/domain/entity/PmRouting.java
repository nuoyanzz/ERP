package com.erp.pm.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 工艺路线：一件物料从备料到完工的标准工序序列。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pm_routing")
public class PmRouting extends BaseEntity {

    public static final int STATUS_DISABLED = 0;
    public static final int STATUS_ENABLED = 1;

    private String routingNo;
    private Long materialId;
    private String version;
    private Integer status;
    private LocalDate effectiveDate;
    private String remark;
}








