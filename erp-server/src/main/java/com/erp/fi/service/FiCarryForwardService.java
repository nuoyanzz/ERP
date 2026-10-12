package com.erp.fi.service;

public interface FiCarryForwardService {

    Long carryForwardProfitAndLoss(String period);

    /** 年末结转：把本年利润余额结转到未分配利润 */
    Long carryForwardYearEnd(String period);

    /** 反结转：作废结转凭证并恢复期间状态 */
    void reverseCarryForward(String period, Integer carryType);
}








