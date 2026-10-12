package com.erp.fi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.fi.domain.entity.FiOpeningBalance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface FiOpeningBalanceMapper extends BaseMapper<FiOpeningBalance> {

    /**
     * 查询指定期间及之前的期初余额合计，按科目汇总。
     * <p>
     * 期初余额以「启用期间」为界，取 period <= 启用期间 的全部录入。
     */
    @Select("""
            SELECT account_id AS accountId,
                   COALESCE(SUM(debit_amount), 0) AS debitAmount,
                   COALESCE(SUM(credit_amount), 0) AS creditAmount
            FROM fi_opening_balance
            WHERE deleted = 0 AND period <= #{period}
            GROUP BY account_id
            """)
    List<Map<String, Object>> sumOpeningBeforePeriod(@Param("period") String period);
}








