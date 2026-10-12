package com.erp.wf.integration;

import com.erp.security.SuperAdminChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 审批流入口守卫。
 * <p>
 * 业务单据的「审核」方法统一在开头调用 {@link #tryStart}：
 * <ul>
 *   <li>返回 {@code true} 表示单据已进入审批流，业务侧应立即返回，由审批通过回调真正执行审核动作</li>
 *   <li>返回 {@code false} 表示该单据类型未配置审批流程（或当前处于审批回调中），业务侧按原逻辑继续审核</li>
 * </ul>
 * 这样业务审核逻辑只有一份，不会出现「审批流一份、直接审核一份」的实现漂移。
 * <p>
 * 工作流服务通过 ApplicationContext 延迟获取：工作流的回调监听器会反向依赖业务 Service，
 * 若此处直接注入会形成循环依赖。
 */
@Component
@RequiredArgsConstructor
public class ApprovalGate implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    private final SuperAdminChecker superAdminChecker;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * 尝试发起审批流程。
     *
     * @param bizType   业务类型，与 wf_definition.biz_type 一致
     * @param bizId     单据 ID
     * @param title     审批标题
     * @param amount    单据金额，用于条件分支求值
     * @param extraVars 额外流程变量，可为 null
     * @return true 表示已进入审批流，业务侧应停止本次审核
     */
    public boolean tryStart(String bizType, Long bizId, String title, BigDecimal amount,
                            Map<String, Object> extraVars) {
        // 审批回调中再次调用审核方法时，不能重复发起流程
        if (ApprovalDispatcher.inCallback()) {
            return false;
        }
        // 超级管理员不受审批流约束，可直接执行审核
        if (superAdminChecker.isCurrentUserSuperAdmin()) {
            return false;
        }
        if (applicationContext == null) {
            return false;
        }
        WorkflowService workflowService;
        try {
            // 延迟获取：业务 Service 正在初始化时此处不能强制解析工作流依赖
            workflowService = applicationContext.getBeanProvider(WorkflowService.class)
                    .getIfAvailable();
        } catch (Exception e) {
            return false;
        }
        if (workflowService == null) {
            return false;
        }
        Long instanceId = workflowService.startApproval(bizType, bizId, title, amount, extraVars);
        return instanceId != null;
    }
}








