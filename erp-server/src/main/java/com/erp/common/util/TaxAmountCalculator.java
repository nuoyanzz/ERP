package com.erp.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 价税分离计算工具。
 * <p>
 * 业务单据上的「金额」是价税合计，但总账需要不含税金额与税额分开记账，
 * 否则存货成本与总账余额口径不一致，增值税也无法核算。
 */
public final class TaxAmountCalculator {

    /** 金额保留位数，与财务科目精度一致 */
    public static final int SCALE = 2;

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private TaxAmountCalculator() {
    }

    /**
     * 按数量、单价、税率拆分价税。
     *
     * @param qty     数量
     * @param price   不含税单价
     * @param taxRate 税率（百分数，如 13 表示 13%）
     * @return 长度为 3 的数组：[不含税金额, 税额, 价税合计]
     */
    public static BigDecimal[] split(BigDecimal qty, BigDecimal price, BigDecimal taxRate) {
        BigDecimal safeQty = qty == null ? BigDecimal.ZERO : qty;
        BigDecimal safePrice = price == null ? BigDecimal.ZERO : price;
        BigDecimal safeRate = taxRate == null ? BigDecimal.ZERO : taxRate;

        BigDecimal netAmount = safeQty.multiply(safePrice).setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal taxAmount = netAmount.multiply(safeRate)
                .divide(ONE_HUNDRED, SCALE, RoundingMode.HALF_UP);
        BigDecimal totalAmount = netAmount.add(taxAmount).setScale(SCALE, RoundingMode.HALF_UP);
        return new BigDecimal[]{netAmount, taxAmount, totalAmount};
    }

    /**
     * 从价税合计金额反推不含税金额与税额。
     * <p>
     * 用于只有总额没有单价的场景（如按订单总额出库）。
     *
     * @param totalAmount 价税合计
     * @param taxRate     税率（百分数）
     * @return 长度为 2 的数组：[不含税金额, 税额]
     */
    public static BigDecimal[] splitFromTotal(BigDecimal totalAmount, BigDecimal taxRate) {
        BigDecimal safeTotal = totalAmount == null ? BigDecimal.ZERO : totalAmount;
        BigDecimal safeRate = taxRate == null ? BigDecimal.ZERO : taxRate;
        BigDecimal divisor = BigDecimal.ONE.add(safeRate.divide(ONE_HUNDRED, 8, RoundingMode.HALF_UP));
        BigDecimal netAmount = safeTotal.divide(divisor, SCALE, RoundingMode.HALF_UP);
        BigDecimal taxAmount = safeTotal.subtract(netAmount).setScale(SCALE, RoundingMode.HALF_UP);
        return new BigDecimal[]{netAmount, taxAmount};
    }

    /**
     * 计算不含税金额。
     */
    public static BigDecimal netAmount(BigDecimal qty, BigDecimal price) {
        BigDecimal safeQty = qty == null ? BigDecimal.ZERO : qty;
        BigDecimal safePrice = price == null ? BigDecimal.ZERO : price;
        return safeQty.multiply(safePrice).setScale(SCALE, RoundingMode.HALF_UP);
    }
}








