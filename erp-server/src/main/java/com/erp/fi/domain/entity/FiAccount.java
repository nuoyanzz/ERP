package com.erp.fi.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fi_account")
public class FiAccount extends BaseEntity {

    public static final int CATEGORY_ASSET = 1;
    public static final int CATEGORY_LIABILITY = 2;
    public static final int CATEGORY_EQUITY = 3;
    public static final int CATEGORY_COST = 4;
    public static final int CATEGORY_PROFIT = 5;

    public static final int BALANCE_DIR_DEBIT = 1;
    public static final int BALANCE_DIR_CREDIT = 2;

    /** 现金流量分类：非现金流 */
    public static final int CASH_FLOW_NONE = 0;
    /** 经营活动 */
    public static final int CASH_FLOW_OPERATING = 1;
    /** 投资活动 */
    public static final int CASH_FLOW_INVESTING = 2;
    /** 筹资活动 */
    public static final int CASH_FLOW_FINANCING = 3;

    private String subjectCode;
    private String subjectName;
    private Long parentId;
    private Integer category;
    private Integer balanceDir;
    private Integer assistCustomer;
    private Integer assistSupplier;
    private Integer assistDept;
    private Integer assistProject;
    private String currency;
    private Integer status;
    private String remark;

    /** 是否明细科目；父科目不可记账 */
    private Integer isLeaf;
    /** 现金流量表分类 */
    private Integer cashFlowType;
    /** 是否参与损益结转；以前年度损益调整等不参与 */
    private Integer carryForward;
    /** 是否数量核算 */
    private Integer quantityAccount;
    private String unit;
    /** 是否外币科目 */
    private Integer foreignCurrency;
}








