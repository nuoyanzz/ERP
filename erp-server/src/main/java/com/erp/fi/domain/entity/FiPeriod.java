package com.erp.fi.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fi_period")
public class FiPeriod extends BaseEntity {

    public static final int STATUS_NOT_OPEN = 0;
    public static final int STATUS_OPEN = 1;
    public static final int STATUS_CLOSED = 2;

    private Integer periodYear;
    private Integer periodNo;
    private String periodMonth;
    private Integer status;
    private String remark;

    /** 结账人 */
    private Long closeBy;
    private LocalDateTime closeTime;
    /** 反结账人 */
    private Long openBy;
    private LocalDateTime openTime;
    /** 是否已执行损益结转 */
    private Integer carriedForward;
    /** 是否年末（12 月） */
    private Integer isYearEnd;
}








