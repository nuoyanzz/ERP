package com.erp.sa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.sa.domain.entity.SaOutstock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SaOutstockMapper extends BaseMapper<SaOutstock> {

    @Select("""
            SELECT outstock_no FROM sa_outstock
            WHERE outstock_no LIKE CONCAT(#{prefix}, '%') AND deleted = 0
            ORDER BY outstock_no DESC LIMIT 1
            """)
    String selectLastOutstockNo(@Param("prefix") String prefix);
}








