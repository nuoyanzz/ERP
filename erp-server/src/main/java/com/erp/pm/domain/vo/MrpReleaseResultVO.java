package com.erp.pm.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * MRP 投放结果。
 */
@Data
public class MrpReleaseResultVO {

    private String runNo;

    /** 成功投放条数 */
    private int releasedCount;

    /** 跳过条数（已投放、无 BOM、无供应商等） */
    private int skippedCount;

    /** 生成的单据明细 */
    private List<MrpReleasedOrderVO> orders = new ArrayList<>();

    /** 跳过原因说明 */
    private List<String> messages = new ArrayList<>();
}








