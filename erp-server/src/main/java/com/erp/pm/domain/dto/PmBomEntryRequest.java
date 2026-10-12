package com.erp.pm.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PmBomEntryRequest {

    @NotNull(message = "子项物料不能为空")
    private Long materialId;

    @NotNull(message = "用量不能为空")
    @DecimalMin(value = "0.0001", message = "用量必须大于0")
    private BigDecimal qty;

    @DecimalMin(value = "0", message = "损耗率不能小于0")
    private BigDecimal lossRate;

    /** 是否替代料：1 表示主料缺货时可用 substituteMaterialId 顶替 */
    private Integer substituteFlag;

    /** 可替代物料 */
    private Long substituteMaterialId;

    /** 明细排序号 */
    private Integer positionNo;

    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;
}








