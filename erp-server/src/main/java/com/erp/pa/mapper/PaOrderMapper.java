package com.erp.pa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.pa.domain.entity.PaOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PaOrderMapper extends BaseMapper<PaOrder> {

    @Select("""
            SELECT order_no FROM pa_order
            WHERE order_no LIKE CONCAT(#{prefix}, '%') AND deleted = 0
            ORDER BY order_no DESC LIMIT 1
            """)
    String selectLastOrderNo(@Param("prefix") String prefix);
}








