package com.erp.ic.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ic_stock")
public class IcStock extends BaseEntity {

    private Long materialId;
    private Long warehouseId;
    private String batchNo;
    private BigDecimal qty;
    private BigDecimal lockedQty;
    private BigDecimal availableQty;
    /** 在途量，采购已下达未入库的部分 */
    private BigDecimal inTransitQty;
    /** 库存金额 */
    private BigDecimal amount;
    /** 加权平均单价 */
    private BigDecimal avgPrice;
}








