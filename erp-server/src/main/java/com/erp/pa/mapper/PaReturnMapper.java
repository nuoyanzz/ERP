package com.erp.pa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.pa.domain.entity.PaReturn;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PaReturnMapper extends BaseMapper<PaReturn> {

    @Select("""
            SELECT return_no FROM pa_return
            WHERE return_no LIKE CONCAT(#{prefix}, '%') AND deleted = 0
            ORDER BY return_no DESC LIMIT 1
            """)
    String selectLastReturnNo(@Param("prefix") String prefix);
}








