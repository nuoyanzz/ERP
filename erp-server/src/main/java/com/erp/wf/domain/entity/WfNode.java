package com.erp.wf.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程节点定义。
 * <p>
 * 节点既承载「审批」语义，也承载「路由」语义：condition_expr 决定该节点在本次实例中是否参与流转，
 * node_type 决定节点在流转中的角色。
 */
@Data
@TableName("wf_node")
public class WfNode {

    /** 发起节点：仅用于标识流程入口，不产生待办 */
    public static final int TYPE_START = 1;
    /** 审批节点 */
    public static final int TYPE_APPROVAL = 2;
    /** 条件分支节点：按 condition_expr 选择后续分支 */
    public static final int TYPE_CONDITION = 3;
    /** 结束节点 */
    public static final int TYPE_END = 4;
    /** 抄送节点：产生抄送记录但不阻塞流转 */
    public static final int TYPE_CC = 5;
    /** 并行分支节点：其后的一组节点同时激活 */
    public static final int TYPE_PARALLEL = 6;

    /** 指定角色 */
    public static final int APPROVER_TYPE_ROLE = 1;
    /** 指定用户 */
    public static final int APPROVER_TYPE_USER = 2;
    /** 发起人本人 */
    public static final int APPROVER_TYPE_STARTER = 3;
    /** 发起人直属主管 */
    public static final int APPROVER_TYPE_STARTER_LEADER = 4;
    /** 指定部门负责人 */
    public static final int APPROVER_TYPE_DEPT_LEADER = 5;
    /** 角色/主管多级链：approver_value 形如 3,2 表示先角色 3 再角色 2 */
    public static final int APPROVER_TYPE_ROLE_CHAIN = 6;

    /** 会签：任一通过即可 */
    public static final int MULTI_ANY = 1;
    /** 会签：全部通过 */
    public static final int MULTI_ALL = 2;

    /** 链式审批中的「直属主管」标记 */
    public static final String CHAIN_TOKEN_LEADER = "leader";

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long definitionId;
    private String nodeKey;
    private String nodeName;
    private Integer nodeType;
    private Integer sortNo;
    private Integer approverType;
    private String approverValue;
    private Integer multiApprove;
    /** 条件表达式，为空表示无条件参与流转 */
    private String conditionExpr;
    /** 超时小时数，大于 0 时计算待办超时时间 */
    private Integer timeoutHours;
    /** 解析不到审批人时的兜底审批人（逗号分隔用户 ID） */
    private String defaultAssignee;
    /** 是否抄送：1=该节点审批人同时收到抄送 */
    private Integer ccFlag;
    private String remark;
    private LocalDateTime createTime;
    @TableLogic
    private Integer deleted;
}








