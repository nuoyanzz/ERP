package com.erp.ar.service;

import com.erp.ar.domain.vo.ArReconVO;

public interface ArReconService {

    /**
     * 往来对账。
     * <p>
     * 输出指定客户的应收单余额合计、总账 1122 科目余额及两者差异。
     *
     * @param customerId 客户 ID
     * @return 对账结果
     */
    ArReconVO recon(Long customerId);
}













