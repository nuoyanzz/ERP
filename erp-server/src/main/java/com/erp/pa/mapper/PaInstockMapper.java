package com.erp.pa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.pa.domain.entity.PaInstock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PaInstockMapper extends BaseMapper<PaInstock> {

    @Select("""
            SELECT instock_no FROM pa_instock
            WHERE instock_no LIKE CONCAT(#{prefix}, '%') AND deleted = 0
            ORDER BY instock_no DESC LIMIT 1
            """)
    String selectLastInstockNo(@Param("prefix") String prefix);
}








