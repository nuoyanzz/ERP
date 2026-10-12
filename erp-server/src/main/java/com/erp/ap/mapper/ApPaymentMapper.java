package com.erp.ap.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.ap.domain.entity.ApPayment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ApPaymentMapper extends BaseMapper<ApPayment> {

    @Select("""
            SELECT payment_no FROM ap_payment
            WHERE payment_no LIKE CONCAT(#{prefix}, '%') AND deleted = 0
            ORDER BY payment_no DESC LIMIT 1
            """)
    String selectLastPaymentNo(@Param("prefix") String prefix);
}








