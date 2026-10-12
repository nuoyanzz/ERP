package com.erp.pm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.pm.domain.entity.PmPick;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PmPickMapper extends BaseMapper<PmPick> {

    @Select("""
            SELECT pick_no FROM pm_pick
            WHERE pick_no LIKE CONCAT(#{prefix}, '%') AND deleted = 0
            ORDER BY pick_no DESC LIMIT 1
            """)
    String selectLastPickNo(@Param("prefix") String prefix);
}








