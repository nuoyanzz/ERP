package com.erp.fi.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 试算平衡表。生成后作为该期间的记账快照，用于结账前的平衡校验。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fi_trial_balance")
public class FiTrialBalance extends BaseEntity {

    private String period;
    private Long accountId;
    private BigDecimal debitAmount;
    private BigDecimal creditAmount;
}








