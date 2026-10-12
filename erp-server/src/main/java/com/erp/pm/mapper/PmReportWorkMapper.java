package com.erp.pm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.pm.domain.entity.PmReportWork;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PmReportWorkMapper extends BaseMapper<PmReportWork> {

    @Select("""
            SELECT report_no FROM pm_report_work
            WHERE report_no LIKE CONCAT(#{prefix}, '%') AND deleted = 0
            ORDER BY report_no DESC LIMIT 1
            """)
    String selectLastReportNo(@Param("prefix") String prefix);
}








