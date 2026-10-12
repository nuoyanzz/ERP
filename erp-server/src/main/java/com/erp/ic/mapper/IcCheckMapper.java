package com.erp.ic.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.ic.domain.entity.IcCheck;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface IcCheckMapper extends BaseMapper<IcCheck> {

    @Select("""
            SELECT check_no FROM ic_check
            WHERE check_no LIKE CONCAT(#{prefix}, '%') AND deleted = 0
            ORDER BY check_no DESC LIMIT 1
            """)
    String selectLastCheckNo(@Param("prefix") String prefix);
}








