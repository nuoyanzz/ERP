package com.erp.ar.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

/**
 * 往来对账的总账侧统计查询。
 * <p>
 * 应收单余额由 {@code ArReceivableMapper} 的实体查询提供（自动套用数据权限）；
 * 本 Mapper 只负责总账侧：fi_voucher_entry join fi_voucher 与 fi_opening_balance。
 */
@Mapper
public interface ArReconMapper {

    /**
     * 总账 1122「应收账款」科目下该客户的已记账发生净额（借正贷负）。
     * <p>
     * 只统计已记账（status=3）的凭证明细，并按辅助核算 CUSTOMER + aux_id 过滤客户。
     */
    @Select("""
            SELECT COALESCE(SUM(e.amount * CASE WHEN e.direction = 1 THEN 1 ELSE -1 END), 0)
            FROM fi_voucher_entry e
            INNER JOIN fi_voucher v ON v.id = e.voucher_id AND v.deleted = 0
            INNER JOIN fi_account a ON a.id = e.account_id AND a.deleted = 0
            WHERE v.status = 3 AND a.subject_code = '1122'
              AND e.aux_type = 'CUSTOMER' AND e.aux_id = #{customerId}
            """)
    BigDecimal sumLedgerMovement(@Param("customerId") Long customerId);

    /**
     * 总账 1122「应收账款」科目下该客户的期初余额（借正贷负）。
     * <p>
     * 期初余额同样带客户辅助核算，一并计入，否则总账口径与科目余额表不一致。
     */
    @Select("""
            SELECT COALESCE(SUM(ob.debit_amount - ob.credit_amount), 0)
            FROM fi_opening_balance ob
            INNER JOIN fi_account a ON a.id = ob.account_id AND a.deleted = 0
            WHERE ob.deleted = 0 AND a.subject_code = '1122'
              AND ob.aux_type = 'CUSTOMER' AND ob.aux_id = #{customerId}
            """)
    BigDecimal sumLedgerOpening(@Param("customerId") Long customerId);
}













