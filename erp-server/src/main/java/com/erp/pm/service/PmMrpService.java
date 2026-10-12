package com.erp.pm.service;

import com.erp.common.api.PageResult;
import com.erp.pm.domain.vo.MrpReleaseResultVO;
import com.erp.pm.domain.vo.MrpSuggestionVO;

import java.util.List;

public interface PmMrpService {

    List<MrpSuggestionVO> runMrp();

    PageResult<MrpSuggestionVO> pageResult(int pageNum, int pageSize, String runNo, Long materialId, Integer suggestType);

    String latestRunNo();

    /**
     * 投放采购订单：把该批次中「采购建议」且未投放的结果生成采购订单。
     *
     * @param runNo 运算批次号
     * @return 投放结果（生成的单据与跳过原因）
     */
    MrpReleaseResultVO releasePurchaseOrder(String runNo);

    /**
     * 投放生产订单：把该批次中「生产建议」且未投放的结果生成生产订单。
     *
     * @param runNo 运算批次号
     * @return 投放结果（生成的单据与跳过原因）
     */
    MrpReleaseResultVO releaseProductionOrder(String runNo);
}








