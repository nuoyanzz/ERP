package com.erp.pm.mapper;

import com.erp.pm.domain.model.PmMaterialPlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 物料计划属性查询。放在 pm 模块内，避免改动 md 模块。
 */
@Mapper
public interface PmMaterialPlanMapper {

    @Select("""
            <script>
            SELECT id, material_code AS materialCode, material_name AS materialName, spec, unit,
                   cost_price AS costPrice, safe_stock AS safeStock, procure_type AS procureType,
                   lead_time_days AS leadTimeDays, purchase_lead_days AS purchaseLeadDays,
                   production_lead_days AS productionLeadDays, min_order_qty AS minOrderQty,
                   lot_size AS lotSize, tax_rate AS taxRate, default_supplier_id AS defaultSupplierId,
                   status
            FROM md_material
            WHERE deleted = 0 AND id IN
            <foreach collection="materialIds" item="materialId" open="(" separator="," close=")">#{materialId}</foreach>
            </script>
            """)
    List<PmMaterialPlan> selectPlans(@Param("materialIds") List<Long> materialIds);
}








