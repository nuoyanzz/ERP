package com.erp.pm.service;

import com.erp.pm.domain.entity.PmBom;

import java.time.LocalDate;
import java.util.List;

/**
 * BOM 版本选择器。
 * <p>
 * 同一物料可能存在多个 BOM 版本，MRP 与生产订单都必须按「需求日」挑出唯一可用的版本，
 * 否则同一物料在不同批次算出的子件需求会不一致。
 */
public interface PmBomSelector {

    /**
     * 按需求日选择某物料可用的 BOM。
     * <p>
     * 选择规则（全部满足）：
     * <ol>
     *   <li>status = 启用</li>
     *   <li>is_default = 1（同一物料同一时刻只允许一个默认版本）</li>
     *   <li>effective_date 为空或 effective_date <= 需求日</li>
     *   <li>expiry_date 为空或 expiry_date >= 需求日</li>
     * </ol>
     * 若默认版本不在有效期内，则退而选取有效期内版本号最大的一个。
     *
     * @param materialId 物料
     * @param demandDate 需求日
     * @return 可用 BOM；没有可用 BOM 时返回 null
     */
    PmBom select(Long materialId, LocalDate demandDate);

    /**
     * 批量选择：返回「物料 → BOM」映射，只包含有可用 BOM 的物料。
     */
    java.util.Map<Long, PmBom> selectAll(List<Long> materialIds, LocalDate demandDate);
}








