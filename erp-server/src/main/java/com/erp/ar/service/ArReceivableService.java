package com.erp.ar.service;

import com.erp.ar.domain.dto.ArReceivableQueryRequest;
import com.erp.ar.domain.vo.ArAgingVO;
import com.erp.ar.domain.vo.ArReceivableVO;
import com.erp.common.api.PageResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ArReceivableService {

    PageResult<ArReceivableVO> pageReceivable(ArReceivableQueryRequest request);

    ArReceivableVO getReceivableDetail(Long id);

    void createFromOutstock(Long customerId, Long sourceId, String sourceNo, BigDecimal amount,
                          LocalDate bizDate, Long deptId);

    /**
     * 冲销应收：销售退货时按来源出库单冲减应收余额。
     *
     * @param sourceType 来源单据类型
     * @param sourceId   来源单据 ID
     * @param amount     冲销金额
     * @return 实际冲销金额
     */
    BigDecimal reverseBySource(String sourceType, Long sourceId, BigDecimal amount);

    List<ArAgingVO> aging(LocalDate asOfDate);
}








