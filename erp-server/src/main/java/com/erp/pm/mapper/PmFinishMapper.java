package com.erp.pm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.pm.domain.entity.PmFinish;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PmFinishMapper extends BaseMapper<PmFinish> {

    @Select("""
            SELECT finish_no FROM pm_finish
            WHERE finish_no LIKE CONCAT(#{prefix}, '%') AND deleted = 0
            ORDER BY finish_no DESC LIMIT 1
            """)
    String selectLastFinishNo(@Param("prefix") String prefix);
}








