package com.erp.wf.engine;

/**
 * 工作流通用常量。
 */
public final class WfConstants {

    private WfConstants() {
    }

    /** 退回次数上限：超过则强制终止流程，防止发起人与审批人反复退回形成死循环 */
    public static final int MAX_ROLLBACK = 3;

    /** 加签位置：前加签，加签人先于原审批人处理 */
    public static final int COUNTERSIGN_BEFORE = 1;
    /** 加签位置：后加签，加签人后于原审批人处理 */
    public static final int COUNTERSIGN_AFTER = 2;

    /** 流程变量：金额 */
    public static final String VAR_AMOUNT = "amount";
    /** 流程变量：单据类型 */
    public static final String VAR_TYPE = "type";
    /** 流程变量：部门 */
    public static final String VAR_DEPT = "dept";
    /** 流程变量：发起人 */
    public static final String VAR_USER = "user";
    /** 流程变量：业务主键 */
    public static final String VAR_BIZ_ID = "bizId";
    /** 流程变量：标题 */
    public static final String VAR_TITLE = "title";
}








