package com.erp.pm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.pm.domain.entity.PmMrpResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PmMrpResultMapper extends BaseMapper<PmMrpResult> {

    @Select("""
            SELECT run_no FROM pm_mrp_result
            WHERE run_no LIKE CONCAT(#{prefix}, '%') AND deleted = 0
            ORDER BY run_no DESC LIMIT 1
            """)
    String selectLastRunNo(@Param("prefix") String prefix);
}








