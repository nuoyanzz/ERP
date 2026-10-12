package com.erp.pm.service;

import com.erp.pm.domain.entity.PmManufacturingCost;
import com.erp.pm.domain.entity.PmMo;
import com.erp.pm.domain.entity.PmReportWork;
import com.erp.pm.domain.model.PmFinishCostAllocation;
import com.erp.pm.domain.model.PmMoCostSummary;

import java.math.BigDecimal;
import java.util.List;

/**
 * 生产成本计算与归集服务。
 * <p>
 * 完工成本结转、报工成本归集都依赖这里，保证材料成本结转口径唯一。
 */
public interface PmCostService {

    /**
     * 计算某工单本次完工应结转的材料成本（移动平均法）。
     * <p>
     * 公式（按累计已完工产量比例结转）：
     * <pre>
     * 累计应结转 = 累计领料成本 × (本次完工后累计完工量 ÷ 订单数量)
     * 本次应结转 = 累计应结转 − 之前已结转材料成本
     * </pre>
     * <p>
     * 之所以要减去「之前已结转」，是因为分批完工时每次都会重新按比例算出累计应结转总额，
     * 只有做差额才能保证整个工单累计结转额恰好等于累计领料成本，不会重复摊销。
     *
     * @param moId      工单 ID
     * @param finishQty 本次完工数量
     * @param orderQty  订单数量
     * @return 本次应结转的材料成本（4 位小数，不会为负）
     */
    BigDecimal calcFinishMaterialCost(Long moId, BigDecimal finishQty, BigDecimal orderQty);

    /**
     * 计算本次完工应结转的完整成本构成（材料 + 人工 + 制造费用）。
     * <p>
     * 人工与制造费用同样按累计完工产量比例从工单成本池中分配，
     * 与材料成本保持同一口径，避免出现材料已结转、人工还挂在在制品上的错配。
     */
    PmFinishCostAllocation calcFinishCostAllocation(Long moId, BigDecimal finishQty, BigDecimal orderQty);

    /**
     * 计算工单的整体成本构成（材料 + 人工 + 制造费用 + 其他）。
     */
    PmMoCostSummary summarizeMoCost(Long moId);

    /**
     * 报工审核后归集人工成本与制造费用，并回写工单成本字段。
     *
     * @param report       报工单
     * @param laborRate    人工费率（元/小时）
     * @param overheadRate 制造费用率（元/小时）
     */
    void collectReportCost(PmReportWork report, BigDecimal laborRate, BigDecimal overheadRate);

    /**
     * 回写工单累计成本字段。
     */
    void refreshMoCost(PmMo mo);

    /**
     * 查询某工单由指定来源单据归集的成本记录。
     * <p>
     * 报工单详情需要反查自己归集了多少人工成本与制造费用。
     *
     * @param moId       工单 ID
     * @param sourceId   来源单据 ID（报工单 ID）
     */
    List<PmManufacturingCost> listReportCosts(Long moId, Long sourceId);
}








