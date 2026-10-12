package com.erp.pm.service;

import com.erp.common.api.PageResult;
import com.erp.pm.domain.dto.PmReportWorkSaveRequest;
import com.erp.pm.domain.vo.PmReportWorkVO;

/**
 * 生产报工。
 */
public interface PmReportWorkService {

    PageResult<PmReportWorkVO> pageReport(int pageNum, int pageSize, Long moId, Integer status, String reportNo);

    PmReportWorkVO getReportDetail(Long id);

    Long createReport(PmReportWorkSaveRequest request);

    void updateReport(PmReportWorkSaveRequest request);

    void deleteReport(Long id);

    /**
     * 审核报工单：
     * <ul>
     *   <li>回写生产订单累计完工量</li>
     *   <li>归集人工成本与制造费用到 pm_manufacturing_cost</li>
     *   <li>生成凭证：借 生产成本(500101)，贷 应付职工薪酬(221101)</li>
     * </ul>
     */
    void auditReport(Long id);
}








