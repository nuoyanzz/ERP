package com.erp.pm.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pm_bom")
public class PmBom extends BaseEntity {

    public static final int STATUS_DISABLED = 0;
    public static final int STATUS_ENABLED = 1;

    private String bomNo;
    private Long materialId;
    private String version;
    private Integer status;

    /**
     * 基准数量：BOM 明细用量是「产出 baseQty 个父件所消耗的子件数量」。
     * 同一物料不同版本的 baseQty 可以不同，展开子件需求时必须按此换算，
     * 否则不同基准产出的 BOM 展开结果会互相污染。
     */
    private BigDecimal baseQty;

    /** 生效日期：MRP 取 BOM 要求 effectiveDate <= 需求日 */
    private LocalDate effectiveDate;

    /** 失效日期：为空表示长期有效；非空时要求 expiryDate >= 需求日 */
    private LocalDate expiryDate;

    /** 是否默认版本：同一物料同一时刻只允许一个 isDefault=1 */
    private Integer isDefault;

    private String remark;
}








