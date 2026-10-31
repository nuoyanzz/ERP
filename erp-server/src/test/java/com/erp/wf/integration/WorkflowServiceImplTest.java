package com.erp.wf.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.security.LoginUser;
import com.erp.wf.domain.entity.WfDefinition;
import com.erp.wf.domain.entity.WfInstance;
import com.erp.wf.mapper.WfDefinitionMapper;
import com.erp.wf.mapper.WfInstanceMapper;
import com.erp.wf.mapper.WfTaskMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 工作流集成服务单元测试。
 * <p>
 * 验证「未配置流程时返回 null」这一关键契约：业务侧据此判断是否跳过审批流。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkflowServiceImplTest {

    @Mock
    private WfDefinitionMapper definitionMapper;
    @Mock
    private WfInstanceMapper instanceMapper;
    @Mock
    private WfTaskMapper taskMapper;

    private WorkflowServiceImpl workflowService;

    @BeforeEach
    void setUp() {
        ApprovalCallbackRegistry registry = new ApprovalCallbackRegistry(List.of());
        workflowService = new WorkflowServiceImpl(definitionMapper, instanceMapper, taskMapper,
                null, registry, null);

        LoginUser loginUser = LoginUser.builder().userId(7L).username("tester").build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("业务类型未配置已发布流程时返回 null，业务侧可跳过审批")
    void startApprovalReturnsNullWhenNoDefinition() {
        when(definitionMapper.selectList(any())).thenReturn(List.of());

        Long instanceId = workflowService.startApproval("PA_ORDER", 1L, "采购订单",
                new BigDecimal("1000"), null);

        assertNull(instanceId);
    }

    @Test
    @DisplayName("业务类型为空时直接返回 null")
    void startApprovalReturnsNullWhenBizTypeBlank() {
        Long instanceId = workflowService.startApproval("  ", 1L, "采购订单",
                new BigDecimal("1000"), null);

        assertNull(instanceId);
    }

    @Test
    @DisplayName("isPending：运行中的流程才算待审批")
    void isPendingChecksRunningInstance() {
        when(instanceMapper.selectCount(any())).thenReturn(1L);
        assertEquals(true, workflowService.isPending("PA_ORDER", 1L));

        when(instanceMapper.selectCount(any())).thenReturn(0L);
        assertEquals(false, workflowService.isPending("PA_ORDER", 1L));
    }

    @Test
    @DisplayName("findPublishedDefinition：按 bizType 与发布状态过滤")
    void findPublishedDefinitionFiltersByBizTypeAndStatus() {
        WfDefinition definition = new WfDefinition();
        definition.setId(99101L);
        definition.setBizType("PA_ORDER");
        definition.setStatus(WfDefinition.STATUS_ENABLED);
        when(definitionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(definition));

        WfDefinition found = workflowService.findPublishedDefinition("PA_ORDER");

        assertEquals(99101L, found.getId());
    }
}








