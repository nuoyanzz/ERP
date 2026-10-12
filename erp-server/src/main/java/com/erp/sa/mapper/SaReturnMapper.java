package com.erp.sa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.sa.domain.entity.SaReturn;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SaReturnMapper extends BaseMapper<SaReturn> {

    @Select("""
            SELECT return_no FROM sa_return
            WHERE return_no LIKE CONCAT(#{prefix}, '%') AND deleted = 0
            ORDER BY return_no DESC LIMIT 1
            """)
    String selectLastReturnNo(@Param("prefix") String prefix);
}








