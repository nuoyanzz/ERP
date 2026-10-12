package com.erp.ar.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.ar.domain.entity.ArReceivable;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ArReceivableMapper extends BaseMapper<ArReceivable> {

    @Select("""
            SELECT receivable_no FROM ar_receivable
            WHERE receivable_no LIKE CONCAT(#{prefix}, '%') AND deleted = 0
            ORDER BY receivable_no DESC LIMIT 1
            """)
    String selectLastReceivableNo(@Param("prefix") String prefix);
}








