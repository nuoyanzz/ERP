package com.erp.pm.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("pm_bom_entry")
public class PmBomEntry {

    private Long id;
    private Long bomId;
    private Long materialId;

    /** 单位用量：每 baseQty 个父件消耗的子件数量（不含损耗） */
    private BigDecimal qty;

    /** 损耗率（百分数），展开子件需求时按 qty * (1 + lossRate/100) 计算 */
    private BigDecimal lossRate;

    /** 是否替代料：1 表示主料缺货时可用 substituteMaterialId 顶替 */
    private Integer substituteFlag;

    /** 可替代物料 */
    private Long substituteMaterialId;

    /** 明细排序号 */
    private Integer positionNo;

    private String remark;
}








