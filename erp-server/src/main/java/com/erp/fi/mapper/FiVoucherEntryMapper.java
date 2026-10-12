package com.erp.fi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.fi.domain.entity.FiVoucherEntry;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface FiVoucherEntryMapper extends BaseMapper<FiVoucherEntry> {

    /**
     * 指定期间已记账凭证的发生额。
     */
    @Select("""
            SELECT e.account_id AS accountId,
                   COALESCE(SUM(CASE WHEN e.direction = 1 THEN e.amount ELSE 0 END), 0) AS debitAmount,
                   COALESCE(SUM(CASE WHEN e.direction = 2 THEN e.amount ELSE 0 END), 0) AS creditAmount
            FROM fi_voucher_entry e
            INNER JOIN fi_voucher v ON v.id = e.voucher_id
            WHERE v.deleted = 0 AND v.status = 3 AND v.period = #{period}
            GROUP BY e.account_id
            """)
    List<Map<String, Object>> sumPostedByPeriod(@Param("period") String period);

    /**
     * 指定期间之前的累计发生额（用于计算期初余额）。
     */
    @Select("""
            SELECT e.account_id AS accountId,
                   COALESCE(SUM(CASE WHEN e.direction = 1 THEN e.amount ELSE 0 END), 0) AS debitAmount,
                   COALESCE(SUM(CASE WHEN e.direction = 2 THEN e.amount ELSE 0 END), 0) AS creditAmount
            FROM fi_voucher_entry e
            INNER JOIN fi_voucher v ON v.id = e.voucher_id
            WHERE v.deleted = 0 AND v.status = 3 AND v.period < #{period}
            GROUP BY e.account_id
            """)
    List<Map<String, Object>> sumPostedBeforePeriod(@Param("period") String period);

    /**
     * 期间区间内的发生额。
     */
    @Select("""
            SELECT e.account_id AS accountId,
                   COALESCE(SUM(CASE WHEN e.direction = 1 THEN e.amount ELSE 0 END), 0) AS debitAmount,
                   COALESCE(SUM(CASE WHEN e.direction = 2 THEN e.amount ELSE 0 END), 0) AS creditAmount
            FROM fi_voucher_entry e
            INNER JOIN fi_voucher v ON v.id = e.voucher_id
            WHERE v.deleted = 0 AND v.status = 3 AND v.period BETWEEN #{startPeriod} AND #{endPeriod}
            GROUP BY e.account_id
            """)
    List<Map<String, Object>> sumPostedBetweenPeriods(@Param("startPeriod") String startPeriod,
                                                      @Param("endPeriod") String endPeriod);

    /**
     * 现金流量表取数：按科目的现金流量分类汇总发生额。
     * <p>
     * 流入取贷方发生额，流出取借方发生额——现金类科目借方为减少、贷方为增加。
     */
    @Select("""
            SELECT a.cash_flow_type AS cashFlowType,
                   a.subject_code AS subjectCode,
                   a.subject_name AS subjectName,
                   COALESCE(SUM(CASE WHEN e.direction = 2 THEN e.amount ELSE 0 END), 0) AS inflowAmount,
                   COALESCE(SUM(CASE WHEN e.direction = 1 THEN e.amount ELSE 0 END), 0) AS outflowAmount
            FROM fi_voucher_entry e
            INNER JOIN fi_voucher v ON v.id = e.voucher_id
            INNER JOIN fi_account a ON a.id = e.account_id
            WHERE v.deleted = 0 AND v.status = 3 AND v.period = #{period}
              AND a.cash_flow_type > 0
            GROUP BY a.cash_flow_type, a.subject_code, a.subject_name
            ORDER BY a.cash_flow_type, a.subject_code
            """)
    List<Map<String, Object>> sumCashFlowByPeriod(@Param("period") String period);

    /**
     * 现金流量表累计取数：年初至指定期间。
     */
    @Select("""
            SELECT a.cash_flow_type AS cashFlowType,
                   a.subject_code AS subjectCode,
                   a.subject_name AS subjectName,
                   COALESCE(SUM(CASE WHEN e.direction = 2 THEN e.amount ELSE 0 END), 0) AS inflowAmount,
                   COALESCE(SUM(CASE WHEN e.direction = 1 THEN e.amount ELSE 0 END), 0) AS outflowAmount
            FROM fi_voucher_entry e
            INNER JOIN fi_voucher v ON v.id = e.voucher_id
            INNER JOIN fi_account a ON a.id = e.account_id
            WHERE v.deleted = 0 AND v.status = 3 AND v.period BETWEEN #{startPeriod} AND #{endPeriod}
              AND a.cash_flow_type > 0
            GROUP BY a.cash_flow_type, a.subject_code, a.subject_name
            ORDER BY a.cash_flow_type, a.subject_code
            """)
    List<Map<String, Object>> sumCashFlowBetweenPeriods(@Param("startPeriod") String startPeriod,
                                                         @Param("endPeriod") String endPeriod);
}








