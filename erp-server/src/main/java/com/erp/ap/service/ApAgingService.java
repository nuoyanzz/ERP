package com.erp.ap.service;

import com.erp.ap.domain.vo.ApAgingVO;

import java.time.LocalDate;
import java.util.List;

public interface ApAgingService {

    /**
     * 应付账龄分析。
     * <p>
     * 仅统计未结清（status=0）且金额大于已付金额的应付单，按到期日分桶：
     * 未到期、逾期 30 天内、31-60 天、61-90 天、90 天以上。
     *
     * @param asOfDate 基准日期，为空时取当天
     * @return 按供应商汇总的账龄分布
     */
    List<ApAgingVO> aging(LocalDate asOfDate);
}











