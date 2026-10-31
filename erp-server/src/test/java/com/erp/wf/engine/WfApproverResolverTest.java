package com.erp.wf.engine;

import com.erp.wf.domain.entity.WfNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

/**
 * 审批人解析单元测试。
 * <p>
 * 只依赖 {@link WfDirectory} 抽象，完全隔离数据库，验证 6 种审批人来源与兜底逻辑。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WfApproverResolverTest {

    @Mock
    private WfDirectory directory;

    private WfApproverResolver resolver;

    /** 角色 ID -> 用户 ID 列表 */
    private final Map<Long, List<Long>> roleUsers = new LinkedHashMap<>();
    /** 启用用户 ID 集合 */
    private final List<Long> enabledUsers = new ArrayList<>();
    /** 部门 ID -> 负责人用户 ID 列表 */
    private final Map<Long, List<Long>> deptLeaders = new LinkedHashMap<>();
    /** 部门 ID -> 启用用户 ID 列表（按升序） */
    private final Map<Long, List<Long>> deptUsers = new LinkedHashMap<>();
    /** 用户 ID -> 部门 ID */
    private final Map<Long, Long> userDepts = new LinkedHashMap<>();
    /** 用户 ID -> 昵称 */
    private final Map<Long, String> nicknames = new LinkedHashMap<>();

    @BeforeEach
    void setUp() {
        resolver = new WfApproverResolver(directory);
        roleUsers.clear();
        enabledUsers.clear();
        deptLeaders.clear();
        deptUsers.clear();
        userDepts.clear();
        nicknames.clear();

        when(directory.userIdsByRoleIds(anyCollection())).thenAnswer(invocation -> {
            Collection<Long> roleIds = invocation.getArgument(0);
            List<Long> userIds = new ArrayList<>();
            for (Long roleId : roleIds) {
                userIds.addAll(roleUsers.getOrDefault(roleId, List.of()));
            }
            return userIds;
        });
        when(directory.filterEnabledUsers(anyCollection())).thenAnswer(invocation -> {
            Collection<Long> userIds = invocation.getArgument(0);
            List<Long> result = new ArrayList<>();
            for (Long userId : userIds) {
                if (enabledUsers.contains(userId)) {
                    result.add(userId);
                }
            }
            return result;
        });
        when(directory.deptLeaderUserIds(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            Long deptId = invocation.getArgument(0);
            return deptLeaders.getOrDefault(deptId, List.of());
        });
        when(directory.enabledUserIdsOfDept(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            Long deptId = invocation.getArgument(0);
            return deptUsers.getOrDefault(deptId, List.of());
        });
        when(directory.deptIdOfUser(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            Long userId = invocation.getArgument(0);
            return userDepts.get(userId);
        });
        when(directory.nicknameOfUser(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            Long userId = invocation.getArgument(0);
            return nicknames.get(userId);
        });
    }

    @Test
    @DisplayName("指定角色：解析出角色下全部启用用户")
    void resolveByRoleReturnsAllUsers() {
        WfNode node = new WfNode();
        node.setApproverType(WfNode.APPROVER_TYPE_ROLE);
        node.setApproverValue("3");

        roleUsers.put(3L, List.of(11L, 12L));
        enabledUsers.addAll(List.of(11L, 12L));

        WfApprover approver = resolver.resolve(node, 1L, 100L);

        assertFalse(approver.isEmpty());
        assertEquals(List.of(11L, 12L), approver.getUserIds());
    }

    @Test
    @DisplayName("指定角色：逗号分隔多个角色时取并集")
    void resolveByRoleSupportsMultipleRoles() {
        WfNode node = new WfNode();
        node.setApproverType(WfNode.APPROVER_TYPE_ROLE);
        node.setApproverValue("3,2");

        roleUsers.put(3L, List.of(11L));
        roleUsers.put(2L, List.of(21L));
        enabledUsers.addAll(List.of(11L, 21L));

        WfApprover approver = resolver.resolve(node, 1L, 100L);

        assertEquals(List.of(11L, 21L), approver.getUserIds());
    }

    @Test
    @DisplayName("指定角色：停用用户被过滤掉")
    void resolveByRoleFiltersDisabledUser() {
        WfNode node = new WfNode();
        node.setApproverType(WfNode.APPROVER_TYPE_ROLE);
        node.setApproverValue("3");

        roleUsers.put(3L, List.of(11L, 12L));
        enabledUsers.add(11L);

        WfApprover approver = resolver.resolve(node, 1L, 100L);

        assertEquals(List.of(11L), approver.getUserIds());
    }

    @Test
    @DisplayName("指定用户：解析出配置的用户")
    void resolveByUserReturnsConfiguredUsers() {
        WfNode node = new WfNode();
        node.setApproverType(WfNode.APPROVER_TYPE_USER);
        node.setApproverValue("11,12");

        enabledUsers.addAll(List.of(11L, 12L));

        WfApprover approver = resolver.resolve(node, 1L, 100L);

        assertEquals(List.of(11L, 12L), approver.getUserIds());
    }

    @Test
    @DisplayName("发起人本人：解析为发起人自己")
    void resolveStarterReturnsStarter() {
        WfNode node = new WfNode();
        node.setApproverType(WfNode.APPROVER_TYPE_STARTER);

        userDepts.put(7L, 100L);
        nicknames.put(7L, "张三");

        WfApprover approver = resolver.resolve(node, 7L, 100L);

        assertEquals(List.of(7L), approver.getUserIds());
    }

    @Test
    @DisplayName("直属主管：命中部门负责人")
    void resolveStarterLeaderMatchesDeptLeader() {
        WfNode node = new WfNode();
        node.setApproverType(WfNode.APPROVER_TYPE_STARTER_LEADER);

        deptLeaders.put(100L, List.of(55L));

        WfApprover approver = resolver.resolve(node, 7L, 100L);

        assertEquals(List.of(55L), approver.getUserIds());
    }

    @Test
    @DisplayName("直属主管：部门内无主管时退化为第一个启用用户")
    void resolveStarterLeaderFallsBackToFirstDeptUser() {
        WfNode node = new WfNode();
        node.setApproverType(WfNode.APPROVER_TYPE_STARTER_LEADER);

        deptUsers.put(100L, List.of(31L, 32L));

        WfApprover approver = resolver.resolve(node, 7L, 100L);

        assertEquals(List.of(31L), approver.getUserIds());
    }

    @Test
    @DisplayName("直属主管：发起人未设置部门时按发起人所在部门推导")
    void resolveStarterLeaderUsesStarterDept() {
        WfNode node = new WfNode();
        node.setApproverType(WfNode.APPROVER_TYPE_STARTER_LEADER);

        userDepts.put(7L, 200L);
        deptLeaders.put(200L, List.of(77L));

        WfApprover approver = resolver.resolve(node, 7L, null);

        assertEquals(List.of(77L), approver.getUserIds());
    }

    @Test
    @DisplayName("部门负责人：指定部门 ID 时按该部门查找")
    void resolveDeptLeaderUsesConfiguredDept() {
        WfNode node = new WfNode();
        node.setApproverType(WfNode.APPROVER_TYPE_DEPT_LEADER);
        node.setApproverValue("200");

        deptLeaders.put(200L, List.of(66L));

        WfApprover approver = resolver.resolve(node, 7L, 100L);

        assertEquals(List.of(66L), approver.getUserIds());
    }

    @Test
    @DisplayName("解析不到审批人时返回失败原因，不静默通过")
    void resolveReturnsFailReasonWhenNoApprover() {
        WfNode node = new WfNode();
        node.setApproverType(WfNode.APPROVER_TYPE_ROLE);
        node.setApproverValue("999");

        WfApprover approver = resolver.resolve(node, 1L, 100L);

        assertTrue(approver.isEmpty());
        assertNotNull(approver.getFailReason());
        assertTrue(approver.getFailReason().contains("999"));
    }

    @Test
    @DisplayName("解析不到审批人时使用兜底审批人")
    void resolveFallsBackToDefaultAssignee() {
        WfNode node = new WfNode();
        node.setApproverType(WfNode.APPROVER_TYPE_ROLE);
        node.setApproverValue("999");
        node.setDefaultAssignee("88");

        enabledUsers.add(88L);

        WfApprover approver = resolver.resolve(node, 1L, 100L);

        assertFalse(approver.isEmpty());
        assertEquals(List.of(88L), approver.getUserIds());
    }

    @Test
    @DisplayName("兜底审批人也不可用时仍返回失败原因")
    void resolveFailsWhenDefaultAssigneeUnavailable() {
        WfNode node = new WfNode();
        node.setApproverType(WfNode.APPROVER_TYPE_ROLE);
        node.setApproverValue("999");
        node.setDefaultAssignee("88");

        // 没有启用用户，兜底也不可用
        WfApprover approver = resolver.resolve(node, 1L, 100L);

        assertTrue(approver.isEmpty());
        assertNotNull(approver.getFailReason());
    }

    @Test
    @DisplayName("多级审批链：按顺序返回每一级审批人")
    void resolveChainLevelsReturnsOrderedLevels() {
        roleUsers.put(3L, List.of(11L));
        roleUsers.put(2L, List.of(21L));
        enabledUsers.addAll(List.of(11L, 21L));

        List<List<Long>> levels = resolver.resolveChainLevels("3,2", 7L, 100L);

        assertEquals(2, levels.size());
        assertEquals(List.of(11L), levels.get(0));
        assertEquals(List.of(21L), levels.get(1));
    }

    @Test
    @DisplayName("多级审批链：leader 标记取发起人直属主管")
    void resolveChainLevelsSupportsLeaderToken() {
        deptLeaders.put(100L, List.of(88L));

        List<List<Long>> levels = resolver.resolveChainLevels("leader", 7L, 100L);

        assertEquals(1, levels.size());
        assertEquals(List.of(88L), levels.get(0));
    }
}








