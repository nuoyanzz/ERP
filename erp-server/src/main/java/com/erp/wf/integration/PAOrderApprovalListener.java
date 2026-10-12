package com.erp.wf.integration;

import com.erp.pa.service.PaOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 采购订单审批监听器。
 * <p>
 * 审批通过后调用业务 Service 执行真正的审核动作。
 * <p>
 * 注意：{@code PaOrderService.auditOrder} 需要改造成「审批流感知」的实现——开头调用
 * {@code workflowService.startApproval}，返回非 null 时直接返回；其余审核逻辑保持不变。
 * 之所以不在监听器里复刻审核逻辑，是因为审核逻辑包含状态校验等规则，复刻必然出现两份实现漂移。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PAOrderApprovalListener implements BizApprovalListener {

    /** 业务类型，需与 wf_definition.biz_type 一致 */
    public static final String BIZ_TYPE = "PA_ORDER";

    private final PaOrderService orderService;

    @Override
    public String bizType() {
        return BIZ_TYPE;
    }

    @Override
    public void onApproved(Long bizId, Map<String, Object> vars) {
        log.info("采购订单审批通过，开始执行审核：bizId={}", bizId);
        orderService.auditOrder(bizId);
    }

    @Override
    public void onRejected(Long bizId, String reason, Map<String, Object> vars) {
        log.info("采购订单审批驳回：bizId={}，原因={}", bizId, reason);
    }
}








