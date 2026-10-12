package com.erp.pm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.pm.domain.entity.PmBom;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PmBomMapper extends BaseMapper<PmBom> {

    @Select("""
            SELECT bom_no FROM pm_bom
            WHERE bom_no LIKE CONCAT(#{prefix}, '%') AND deleted = 0
            ORDER BY bom_no DESC LIMIT 1
            """)
    String selectLastBomNo(@Param("prefix") String prefix);
}








