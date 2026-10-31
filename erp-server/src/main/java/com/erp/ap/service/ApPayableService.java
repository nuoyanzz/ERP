package com.erp.ap.service;

import com.erp.ap.domain.dto.ApPayableQueryRequest;
import com.erp.ap.domain.vo.ApPayableVO;
import com.erp.common.api.PageResult;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface ApPayableService {

    PageResult<ApPayableVO> pagePayable(ApPayableQueryRequest request);

    ApPayableVO getPayableDetail(Long id);

    void createFromInstock(Long supplierId, Long sourceId, String sourceNo, BigDecimal amount,
                          LocalDate bizDate, Long deptId);

    /**
     * 冲销应付：采购退货时按来源入库单冲减应付余额。
     *
     * @param sourceType 来源单据类型
     * @param sourceId   来源单据 ID
     * @param amount     冲销金额
     * @return 实际冲销金额
     */
    BigDecimal reverseBySource(String sourceType, Long sourceId, BigDecimal amount);
}








