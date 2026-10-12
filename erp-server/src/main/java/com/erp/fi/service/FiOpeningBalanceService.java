package com.erp.fi.service;

import com.erp.fi.domain.dto.OpeningBalanceRequest;
import com.erp.fi.domain.vo.OpeningBalanceVO;
import com.erp.fi.domain.vo.TrialBalanceVO;

import java.util.List;

/**
 * 期初余额与试算平衡。
 */
public interface FiOpeningBalanceService {

    /**
     * 查询指定期间的期初余额。
     */
    List<OpeningBalanceVO> listOpeningBalances(String period);

    /**
     * 录入期初余额。同科目同辅助项重复录入时覆盖。
     */
    Long saveOpeningBalance(OpeningBalanceRequest request);

    /**
     * 批量导入期初余额。
     */
    int importOpeningBalances(List<OpeningBalanceRequest> requests);

    /**
     * 删除期初余额。
     */
    void deleteOpeningBalance(Long id);

    /**
     * 生成试算平衡表并保存快照。
     */
    TrialBalanceVO generateTrialBalance(String period);
}








