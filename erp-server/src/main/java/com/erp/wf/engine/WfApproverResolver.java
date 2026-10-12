package com.erp.wf.engine;

import com.erp.wf.domain.entity.WfNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 审批人解析器。
 * <p>
 * 支持 6 种审批人来源：指定角色、指定用户、发起人本人、发起人直属主管、指定部门负责人、
 * 角色/主管多级链。解析不到审批人时先尝试节点配置的兜底人，仍拿不到则返回失败原因，由引擎挂起实例。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WfApproverResolver {

    private final WfDirectory directory;

    /**
     * 解析节点审批人。
     *
     * @param node      节点定义
     * @param starterId 发起人用户 ID
     * @param deptId    发起人部门 ID（可为空，为空时按发起人所在部门推导）
     */
    public WfApprover resolve(WfNode node, Long starterId, Long deptId) {
        if (node == null) {
            return WfApprover.fail("节点不存在");
        }
        Integer approverType = node.getApproverType();
        if (approverType == null) {
            approverType = WfNode.APPROVER_TYPE_ROLE;
        }
        String value = node.getApproverValue();
        WfApprover result = switch (approverType) {
            case WfNode.APPROVER_TYPE_ROLE -> resolveByRole(value);
            case WfNode.APPROVER_TYPE_USER -> resolveByUser(value);
            case WfNode.APPROVER_TYPE_STARTER -> resolveStarter(starterId);
            case WfNode.APPROVER_TYPE_STARTER_LEADER -> resolveStarterLeader(starterId, deptId);
            case WfNode.APPROVER_TYPE_DEPT_LEADER -> resolveDeptLeader(deptId, value);
            case WfNode.APPROVER_TYPE_ROLE_CHAIN -> resolveChainFirstLevel(value, starterId, deptId);
            default -> WfApprover.fail("不支持的审批人类型：" + approverType);
        };
        if (!result.isEmpty()) {
            return result;
        }
        return withDefault(node, result.getFailReason());
    }

    /**
     * 应用节点兜底审批人。
     */
    private WfApprover withDefault(WfNode node, String failReason) {
        List<Long> defaults = parseIds(node.getDefaultAssignee());
        if (defaults.isEmpty()) {
            return WfApprover.fail(failReason);
        }
        List<Long> valid = directory.filterEnabledUsers(defaults);
        if (valid.isEmpty()) {
            return WfApprover.fail(failReason + "；兜底审批人不可用");
        }
        return WfApprover.of(valid);
    }

    /**
     * 按角色解析：{@code approver_value} 为角色 ID，支持逗号分隔多个角色（并集，全部审批）。
     */
    private WfApprover resolveByRole(String value) {
        List<Long> roleIds = parseIds(value);
        if (roleIds.isEmpty()) {
            return WfApprover.fail("未配置审批角色");
        }
        List<Long> userIds = directory.userIdsByRoleIds(roleIds);
        if (userIds.isEmpty()) {
            return WfApprover.fail("角色下没有用户：" + roleIds);
        }
        List<Long> valid = directory.filterEnabledUsers(userIds);
        if (valid.isEmpty()) {
            return WfApprover.fail("角色下没有可用用户：" + roleIds);
        }
        return WfApprover.of(valid);
    }

    /**
     * 按用户解析：{@code approver_value} 为用户 ID，支持逗号分隔。
     */
    private WfApprover resolveByUser(String value) {
        List<Long> userIds = parseIds(value);
        if (userIds.isEmpty()) {
            return WfApprover.fail("未配置审批用户");
        }
        List<Long> valid = directory.filterEnabledUsers(userIds);
        if (valid.isEmpty()) {
            return WfApprover.fail("配置的审批用户不可用：" + userIds);
        }
        return WfApprover.of(valid);
    }

    /**
     * 发起人本人。
     */
    private WfApprover resolveStarter(Long starterId) {
        if (starterId == null) {
            return WfApprover.fail("发起人不存在");
        }
        Long deptId = directory.deptIdOfUser(starterId);
        if (deptId == null && directory.nicknameOfUser(starterId) == null) {
            return WfApprover.fail("发起人不存在");
        }
        return WfApprover.of(List.of(starterId));
    }

    /**
     * 发起人直属主管：优先按部门内职级匹配（主管/经理/总监等），
     * 部门内没有主管时退化为同部门第一个启用用户。
     */
    private WfApprover resolveStarterLeader(Long starterId, Long deptId) {
        Long targetDeptId = deptId != null ? deptId : directory.deptIdOfUser(starterId);
        if (targetDeptId == null) {
            return WfApprover.fail("发起人未设置部门，无法定位直属主管");
        }
        List<Long> leaders = directory.deptLeaderUserIds(targetDeptId);
        if (!leaders.isEmpty()) {
            return WfApprover.of(leaders);
        }
        List<Long> deptUsers = directory.enabledUserIdsOfDept(targetDeptId);
        if (!deptUsers.isEmpty()) {
            return WfApprover.of(List.of(deptUsers.get(0)));
        }
        return WfApprover.fail("部门 " + targetDeptId + " 下没有可用用户");
    }

    /**
     * 指定部门负责人：{@code approver_value} 可为空，为空时使用发起人所在部门。
     */
    private WfApprover resolveDeptLeader(Long starterDeptId, String value) {
        List<Long> configured = parseIds(value);
        Long targetDeptId = configured.isEmpty() ? starterDeptId : configured.get(0);
        if (targetDeptId == null) {
            return WfApprover.fail("未配置部门");
        }
        List<Long> leaders = directory.deptLeaderUserIds(targetDeptId);
        if (!leaders.isEmpty()) {
            return WfApprover.of(leaders);
        }
        return WfApprover.fail("部门 " + targetDeptId + " 没有负责人");
    }

    /**
     * 多级链的第一级审批人，用于在节点激活前做一次可用性校验。
     */
    private WfApprover resolveChainFirstLevel(String value, Long starterId, Long deptId) {
        List<List<Long>> levels = resolveChainLevels(value, starterId, deptId);
        if (levels.isEmpty()) {
            return WfApprover.fail("未配置审批链");
        }
        return WfApprover.of(levels.get(0));
    }

    /**
     * 解析多级链：{@code approver_value} 形如 {@code 3,2}，表示先由角色 3 审批，再由角色 2 审批；
     * 元素为 {@code leader} 时表示取发起人直属主管。
     * <p>
     * 链式审批必须逐级流转，因此返回每一级的审批人列表，由引擎按级激活。
     */
    public List<List<Long>> resolveChainLevels(String value, Long starterId, Long deptId) {
        List<List<Long>> levels = new ArrayList<>();
        if (!StringUtils.hasText(value)) {
            return levels;
        }
        for (String token : value.split(",")) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            List<Long> users;
            if (WfNode.CHAIN_TOKEN_LEADER.equalsIgnoreCase(trimmed)) {
                users = resolveStarterLeader(starterId, deptId).getUserIds();
            } else {
                users = resolveByRole(trimmed).getUserIds();
            }
            if (!users.isEmpty()) {
                levels.add(users);
            }
        }
        return levels;
    }

    /**
     * 解析逗号分隔的 ID 列表，忽略非法项。
     */
    private List<Long> parseIds(String value) {
        List<Long> ids = new ArrayList<>();
        if (!StringUtils.hasText(value)) {
            return ids;
        }
        for (String token : value.split(",")) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                ids.add(Long.valueOf(trimmed));
            } catch (NumberFormatException e) {
                log.warn("忽略非法的 ID 配置：{}", trimmed);
            }
        }
        return ids;
    }
}








