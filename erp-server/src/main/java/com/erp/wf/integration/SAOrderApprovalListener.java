package com.erp.wf.integration;

import com.erp.sa.service.SaOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 销售订单审批监听器。
 * <p>
 * 与采购订单同理：{@code SaOrderService.auditOrder} 需在开头接入审批流，审核逻辑保持不变。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SAOrderApprovalListener implements BizApprovalListener {

    /** 业务类型，需与 wf_definition.biz_type 一致 */
    public static final String BIZ_TYPE = "SA_ORDER";

    private final SaOrderService orderService;

    @Override
    public String bizType() {
        return BIZ_TYPE;
    }

    @Override
    public void onApproved(Long bizId, Map<String, Object> vars) {
        log.info("销售订单审批通过，开始执行审核：bizId={}", bizId);
        orderService.auditOrder(bizId);
    }

    @Override
    public void onRejected(Long bizId, String reason, Map<String, Object> vars) {
        log.info("销售订单审批驳回：bizId={}，原因={}", bizId, reason);
    }
}








