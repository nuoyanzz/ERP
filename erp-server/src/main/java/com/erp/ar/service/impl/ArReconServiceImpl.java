package com.erp.ar.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.ar.domain.entity.ArReceivable;
import com.erp.ar.domain.vo.ArReconVO;
import com.erp.ar.mapper.ArReceivableMapper;
import com.erp.ar.mapper.ArReconMapper;
import com.erp.ar.service.ArReconService;
import com.erp.common.exception.BizException;
import com.erp.md.domain.entity.MdCustomer;
import com.erp.md.mapper.MdCustomerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 应收往来对账服务实现。
 * <p>
 * 对账口径：
 * <ul>
 *   <li><b>应收单余额</b>：ar_receivable 中该客户未结清（status=0）单据的「金额 - 已收金额」合计。
 *       这里走实体查询，自动套用数据权限（部门 / 创建人），与账龄分析口径一致；</li>
 *   <li><b>总账余额</b>：总账 1122「应收账款」科目下该客户的辅助核算余额，
 *       等于期初余额（fi_opening_balance，借正贷负）加上已记账凭证的发生净额
 *       （fi_voucher_entry join fi_voucher，status=3，aux_type=CUSTOMER 且 aux_id=客户）。
 *       总账是全局账簿，不套用数据权限；</li>
 *   <li><b>差异</b>：应收单余额 - 总账余额，绝对值不超过 0.01 视为账实相符。</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ArReconServiceImpl implements ArReconService {

    /** 允许的差异容差，金额精确到分 */
    private static final BigDecimal TOLERANCE = new BigDecimal("0.01");

    private final ArReconMapper reconMapper;
    private final ArReceivableMapper receivableMapper;
    private final MdCustomerMapper customerMapper;

    @Override
    public ArReconVO recon(Long customerId) {
        if (customerId == null) {
            throw new BizException("客户不能为空");
        }
        MdCustomer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw new BizException("客户不存在");
        }
        BigDecimal receivableBalance = sumReceivableBalance(customerId);
        BigDecimal ledgerBalance = nz(reconMapper.sumLedgerOpening(customerId))
                .add(nz(reconMapper.sumLedgerMovement(customerId)));
        BigDecimal difference = receivableBalance.subtract(ledgerBalance);

        ArReconVO vo = new ArReconVO();
        vo.setCustomerId(customerId);
        vo.setCustomerName(customer.getCustomerName());
        vo.setReceivableBalance(receivableBalance);
        vo.setLedgerBalance(ledgerBalance);
        vo.setDifference(difference);
        vo.setChecked(difference.abs().compareTo(TOLERANCE) <= 0);
        return vo;
    }

    /**
     * 客户未结清应收余额合计（未收金额）。
     */
    private BigDecimal sumReceivableBalance(Long customerId) {
        List<ArReceivable> receivables = receivableMapper.selectList(Wrappers.<ArReceivable>lambdaQuery()
                .eq(ArReceivable::getCustomerId, customerId)
                .eq(ArReceivable::getStatus, ArReceivable.STATUS_OPEN));
        BigDecimal total = BigDecimal.ZERO;
        for (ArReceivable receivable : receivables) {
            BigDecimal received = receivable.getReceivedAmount() == null
                    ? BigDecimal.ZERO : receivable.getReceivedAmount();
            BigDecimal remaining = receivable.getAmount().subtract(received);
            if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                total = total.add(remaining);
            }
        }
        return total;
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}













