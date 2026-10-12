package com.erp.wf.engine;

import java.util.Collection;
import java.util.List;

/**
 * 审批人解析所需的组织与人事数据查询接口。
 * <p>
 * 单独抽出来的原因：审批人解析是工作流里唯一需要跨模块读数据的地方，
 * 抽成窄接口后解析规则可以脱离数据库独立测试，也让引擎不直接依赖 Mapper。
 */
public interface WfDirectory {

    /**
     * 查询指定角色下的全部用户 ID（不做启用状态过滤）。
     */
    List<Long> userIdsByRoleIds(Collection<Long> roleIds);

    /**
     * 过滤出启用状态的用户 ID，保持传入顺序。
     */
    List<Long> filterEnabledUsers(Collection<Long> userIds);

    /**
     * 查询用户所属部门。
     */
    Long deptIdOfUser(Long userId);

    /**
     * 查询用户昵称。
     */
    String nicknameOfUser(Long userId);

    /**
     * 查询部门内的负责人用户 ID（按职位关键字匹配员工与登录用户）。
     */
    List<Long> deptLeaderUserIds(Long deptId);

    /**
     * 查询部门内的启用用户 ID，按用户 ID 升序。
     */
    List<Long> enabledUserIdsOfDept(Long deptId);
}








