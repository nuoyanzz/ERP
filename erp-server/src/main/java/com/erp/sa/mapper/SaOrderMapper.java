package com.erp.sa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.sa.domain.entity.SaOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SaOrderMapper extends BaseMapper<SaOrder> {

    @Select("""
            SELECT order_no FROM sa_order
            WHERE order_no LIKE CONCAT(#{prefix}, '%') AND deleted = 0
            ORDER BY order_no DESC LIMIT 1
            """)
    String selectLastOrderNo(@Param("prefix") String prefix);
}








