package com.erp.pm.service;

import com.erp.common.api.PageResult;
import com.erp.pm.domain.vo.PmManufacturingCostVO;
import com.erp.pm.domain.vo.PmMoCostVO;

import java.util.List;

/**
 * 生产成本归集查询。
 */
public interface PmManufacturingCostService {

    /**
     * 工单成本汇总列表：人工成本、制造费用、其他成本、合计与单位成本。
     */
    PageResult<PmMoCostVO> pageMoCost(int pageNum, int pageSize, Long moId, Long materialId);

    /**
     * 单个工单的成本详情（含归集明细）。
     */
    PmMoCostVO getMoCostDetail(Long moId);

    /**
     * 成本归集明细分页。
     */
    PageResult<PmManufacturingCostVO> pageCostDetail(int pageNum, int pageSize, Long moId, Integer costType);

    /**
     * 工单成本汇总列表（不分页，供导出使用）。
     */
    List<PmMoCostVO> listMoCost(Long materialId);
}








