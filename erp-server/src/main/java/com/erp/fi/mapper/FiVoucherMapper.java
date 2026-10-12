package com.erp.fi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.fi.domain.entity.FiVoucher;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FiVoucherMapper extends BaseMapper<FiVoucher> {

    /**
     * 查询该期间已用的最大凭证序号。
     * <p>
     * 按数字排序而非字符串排序——凭证号序号超过 9999 后字符串排序会错乱
     * （「记-202601-10000」会排在「记-202601-9999」之前）。
     */
    @Select("""
            SELECT COALESCE(MAX(CAST(SUBSTRING_INDEX(voucher_no, '-', -1) AS UNSIGNED)), 0)
            FROM fi_voucher
            WHERE period = #{period} AND deleted = 0
            """)
    Integer selectMaxSeqByPeriod(String period);
}








