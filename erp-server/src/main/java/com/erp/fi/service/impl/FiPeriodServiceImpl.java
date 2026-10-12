package com.erp.fi.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.exception.BizException;
import com.erp.fi.domain.dto.FiPeriodSaveRequest;
import com.erp.fi.domain.entity.FiCarryForward;
import com.erp.fi.domain.entity.FiPeriod;
import com.erp.fi.domain.entity.FiVoucher;
import com.erp.fi.domain.vo.FiPeriodVO;
import com.erp.fi.mapper.FiCarryForwardMapper;
import com.erp.fi.mapper.FiPeriodMapper;
import com.erp.fi.mapper.FiVoucherMapper;
import com.erp.fi.service.AccountBalanceCalculator;
import com.erp.fi.service.FiPeriodService;
import com.erp.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FiPeriodServiceImpl extends ServiceImpl<FiPeriodMapper, FiPeriod> implements FiPeriodService {

    private final FiVoucherMapper voucherMapper;
    private final FiCarryForwardMapper carryForwardMapper;
    private final AccountBalanceCalculator balanceCalculator;

    @Override
    public List<FiPeriodVO> listByYear(Integer periodYear) {
        List<FiPeriod> periods = this.list(Wrappers.<FiPeriod>lambdaQuery()
                .eq(periodYear != null, FiPeriod::getPeriodYear, periodYear)
                .orderByAsc(FiPeriod::getPeriodNo));
        return periods.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public FiPeriodVO getPeriodDetail(Long id) {
        FiPeriod period = this.getById(id);
        if (period == null) {
            throw new BizException("会计期间不存在");
        }
        return toVO(period);
    }

    @Override
    public int generateYearPeriods(FiPeriodSaveRequest request) {
        int created = 0;
        for (int month = 1; month <= 12; month++) {
            String periodMonth = request.getPeriodYear() + "-" + String.format("%02d", month);
            Long count = this.count(Wrappers.<FiPeriod>lambdaQuery().eq(FiPeriod::getPeriodMonth, periodMonth));
            if (count != null && count > 0) {
                continue;
            }
            FiPeriod period = new FiPeriod();
            period.setPeriodYear(request.getPeriodYear());
            period.setPeriodNo(month);
            period.setPeriodMonth(periodMonth);
            period.setStatus(FiPeriod.STATUS_OPEN);
            period.setRemark(request.getRemark());
            period.setCarriedForward(0);
            period.setIsYearEnd(month == 12 ? 1 : 0);
            this.save(period);
            created++;
        }
        return created;
    }

    /**
     * 结账。
     * <p>
     * 结账是会计期间的封口动作，必须通过三道校验：
     * 无未记账凭证、试算平衡、损益已结转。任何一项不通过都不允许结账，
     * 否则该期间的数据将永久固化而无法纠正。
     */
    @Override
    public void closePeriod(Long id) {
        FiPeriod period = this.getById(id);
        if (period == null) {
            throw new BizException("会计期间不存在");
        }
        if (period.getStatus() == null || period.getStatus() != FiPeriod.STATUS_OPEN) {
            throw new BizException("只有开放状态的期间才能结账");
        }

        Long pendingCount = voucherMapper.selectCount(Wrappers.<FiVoucher>lambdaQuery()
                .eq(FiVoucher::getPeriod, period.getPeriodMonth())
                .in(FiVoucher::getStatus, FiVoucher.STATUS_DRAFT, FiVoucher.STATUS_SUBMITTED,
                        FiVoucher.STATUS_AUDITED));
        if (pendingCount != null && pendingCount > 0) {
            throw new BizException("该期间存在未记账凭证，不能结账");
        }

        AccountBalanceCalculator.TrialBalanceResult trial = balanceCalculator.trialBalance(period.getPeriodMonth());
        if (!trial.isBalanced()) {
            throw new BizException("试算不平衡，借方合计 " + trial.getDebitTotal()
                    + "，贷方合计 " + trial.getCreditTotal() + "，不能结账");
        }

        Long carried = carryForwardMapper.selectCount(Wrappers.<FiCarryForward>lambdaQuery()
                .eq(FiCarryForward::getPeriod, period.getPeriodMonth())
                .eq(FiCarryForward::getCarryType, FiCarryForward.TYPE_PROFIT_LOSS)
                .eq(FiCarryForward::getStatus, FiCarryForward.STATUS_ACTIVE));
        if (carried == null || carried == 0) {
            throw new BizException("该期间尚未执行损益结转，不能结账");
        }

        FiPeriod update = new FiPeriod();
        update.setId(id);
        update.setStatus(FiPeriod.STATUS_CLOSED);
        update.setCloseBy(SecurityUtils.getCurrentUserId());
        update.setCloseTime(LocalDateTime.now());
        this.updateById(update);
    }

    /**
     * 反结账。
     * <p>
     * 反结账会重新开放已封口的期间，必须确认后续期间没有结账，
     * 否则会出现「后期间已结账、前期间被重开」的账序倒置。
     */
    @Override
    public void openPeriod(Long id) {
        FiPeriod period = this.getById(id);
        if (period == null) {
            throw new BizException("会计期间不存在");
        }
        if (period.getStatus() == null || period.getStatus() != FiPeriod.STATUS_CLOSED) {
            throw new BizException("只有已结账状态的期间才能反结账");
        }

        Long laterClosed = this.count(Wrappers.<FiPeriod>lambdaQuery()
                .gt(FiPeriod::getPeriodMonth, period.getPeriodMonth())
                .eq(FiPeriod::getStatus, FiPeriod.STATUS_CLOSED));
        if (laterClosed != null && laterClosed > 0) {
            throw new BizException("存在后续已结账期间，请先依次反结账后续期间");
        }

        FiPeriod update = new FiPeriod();
        update.setId(id);
        update.setStatus(FiPeriod.STATUS_OPEN);
        update.setOpenBy(SecurityUtils.getCurrentUserId());
        update.setOpenTime(LocalDateTime.now());
        this.updateById(update);
    }

    private FiPeriodVO toVO(FiPeriod period) {
        FiPeriodVO vo = new FiPeriodVO();
        BeanUtils.copyProperties(period, vo);
        return vo;
    }
}








