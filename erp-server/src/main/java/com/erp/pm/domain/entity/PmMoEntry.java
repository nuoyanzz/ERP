package com.erp.pm.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("pm_mo_entry")
public class PmMoEntry {

    private Long id;
    private Long moId;
    private Long bomEntryId;
    private Long materialId;
    private BigDecimal requireQty;
    private BigDecimal pickedQty;
    private String remark;
}








