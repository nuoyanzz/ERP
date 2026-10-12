package com.erp.pm.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class PmBomSaveRequest {

    private Long id;

    @NotNull(message = "父项物料不能为空")
    private Long materialId;

    @Size(max = 20, message = "版本号长度不能超过20")
    private String version;

    /** 基准数量：BOM 明细用量对应的产出数量，默认 1 */
    @DecimalMin(value = "0.0001", message = "基准数量必须大于0")
    private BigDecimal baseQty;

    /** 生效日期 */
    private LocalDate effectiveDate;

    /** 失效日期，为空表示长期有效 */
    private LocalDate expiryDate;

    /** 是否默认版本：同一物料同一时刻只允许一个 */
    private Integer isDefault;

    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;

    @NotEmpty(message = "BOM 明细不能为空")
    private List<PmBomEntryRequest> entries;
}








