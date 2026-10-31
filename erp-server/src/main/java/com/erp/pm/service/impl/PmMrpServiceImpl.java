package com.erp.pm.service.impl;

import com.erp.common.util.SerialNoGenerator;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.ic.mapper.IcStockMapper;
import com.erp.md.domain.entity.MdMaterial;
import com.erp.md.domain.entity.MdWarehouse;
import com.erp.md.mapper.MdMaterialMapper;
import com.erp.md.mapper.MdWarehouseMapper;
import com.erp.pa.domain.entity.PaOrder;
import com.erp.pa.domain.entity.PaOrderEntry;
import com.erp.pa.mapper.PaOrderEntryMapper;
import com.erp.pa.mapper.PaOrderMapper;
import com.erp.pm.domain.entity.PmBom;
import com.erp.pm.domain.entity.PmBomEntry;
import com.erp.pm.domain.entity.PmMo;
import com.erp.pm.domain.entity.PmMrpResult;
import com.erp.pm.domain.model.PmMaterialPlan;
import com.erp.pm.domain.vo.MrpReleasedOrderVO;
import com.erp.pm.domain.vo.MrpReleaseResultVO;
import com.erp.pm.domain.vo.MrpSuggestionVO;
import com.erp.pm.mapper.PmBomEntryMapper;
import com.erp.pm.mapper.PmBomMapper;
import com.erp.pm.mapper.PmMaterialPlanMapper;
import com.erp.pm.mapper.PmMoMapper;
import com.erp.pm.mapper.PmMrpResultMapper;
import com.erp.pm.service.PmBomSelector;
import com.erp.pm.service.PmMrpService;
import com.erp.sa.domain.entity.SaOrder;
import com.erp.sa.domain.entity.SaOrderEntry;
import com.erp.sa.mapper.SaOrderEntryMapper;
import com.erp.sa.mapper.SaOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PmMrpServiceImpl implements PmMrpService {

    private static final String NO_PREFIX = "MRP-";
    private static final String PA_ORDER_NO_PREFIX = "CG-";
    private static final String MO_NO_PREFIX = "MO-";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final int SCALE = 4;

    private final PmMrpResultMapper mrpResultMapper;
    private final MdMaterialMapper materialMapper;
    private final MdWarehouseMapper warehouseMapper;
    private final PmMaterialPlanMapper materialPlanMapper;
    private final IcStockMapper stockMapper;
    private final SaOrderMapper saOrderMapper;
    private final SaOrderEntryMapper saOrderEntryMapper;
    private final PaOrderMapper paOrderMapper;
    private final PaOrderEntryMapper paOrderEntryMapper;
    private final PmMoMapper moMapper;
    private final PmBomMapper bomMapper;
    private final PmBomEntryMapper bomEntryMapper;
    private final PmBomSelector bomSelector;
    /** 业务单号生成器（原子自增，避免并发撞号） */
    private final SerialNoGenerator serialNoGenerator;

    // ------------------------------------------------------------
    // MRP 运算：两段式算法
    // ------------------------------------------------------------

    /**
     * MRP 运算（两段式）。
     * <p>
     * <b>第一段（毛需求汇总）</b>：先从销售订单收集独立需求，
     * 再沿 BOM 逐层展开子件需求，把同一物料的所有毛需求<b>先汇总完</b>。
     * <p>
     * <b>第二段（净需求计算）</b>：对汇总后的毛需求统一计算净需求
     * （扣库存、扣在途、加安全库存），再按批量/最小订购量向上取整。
     * <p>
     * 之所以必须分两段：原实现用 processed 集合标记「已处理物料」，
     * 同一物料先以独立需求处理后，后续从上层展开出来的子件需求被合并进 demand map
     * 就不再重算净需求，新增量被永久丢弃，导致多级 BOM 展开结果错误。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<MrpSuggestionVO> runMrp() {
        String runNo = nextRunNo();
        LocalDate demandDate = LocalDate.now();

        Map<Long, BigDecimal> purchaseOnOrderMap = collectPurchaseOnOrder();
        Map<Long, BigDecimal> productionOnOrderMap = collectProductionOnOrder();
        Map<Long, BigDecimal> availableMap = collectAvailableStock();

        // ===== 第一段：按 BOM 拓扑序逐层汇总毛需求并计算净需求 =====
        // grossDemand 汇总所有来源的毛需求。
        // 关键点一：子件需求必须用「父件的净需求（计划量）」而不是父件毛需求来展开，
        //         否则父件库存没有被消耗掉，展开出来的子件需求会偏大。
        // 关键点二：必须按 BOM 拓扑序（父件先于子件）处理。
        //         同一个物料可能既是别的物料的子件、自己又有子件（多级 BOM），
        //         也可能被多个父件同时引用；若按层级循环并用 visited 跳过已处理物料，
        //         后到达的需求贡献会被永久丢弃。用拓扑序保证每个物料只处理一次，
        //         且处理到它时所有父件的贡献都已累加完毕。
        Map<Long, BigDecimal> grossDemand = new LinkedHashMap<>();
        collectSalesDemand(grossDemand);

        Map<Long, List<PmBomEntry>> entriesByBom = loadBomEntries();
        // bomCache 按需求日缓存每个物料选中的 BOM（多级展开时每个物料只选一次）
        Map<Long, PmBom> bomCache = new HashMap<>();
        // plannedQty 记录每个物料的净需求（已按批量取整），作为展开下一层的依据
        Map<Long, BigDecimal> plannedByMaterial = new LinkedHashMap<>();

        // 按拓扑序处理：父件一定排在子件之前
        List<Long> topoOrder = buildTopoOrder(entriesByBom, bomCache, demandDate);
        for (Long materialId : topoOrder) {
            BigDecimal grossQty = grossDemand.getOrDefault(materialId, BigDecimal.ZERO);
            if (grossQty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            MdMaterial material = materialMapper.selectById(materialId);
            if (material == null) {
                continue;
            }
            BigDecimal availableQty = availableMap.getOrDefault(materialId, BigDecimal.ZERO);
            BigDecimal safeStock = material.getSafeStock() == null ? BigDecimal.ZERO : material.getSafeStock();
            BigDecimal onOrderQty = purchaseOnOrderMap.getOrDefault(materialId, BigDecimal.ZERO)
                    .add(productionOnOrderMap.getOrDefault(materialId, BigDecimal.ZERO));
            // 净需求 = 毛需求 + 安全库存 − 可用库存 − 在途量
            BigDecimal netQty = grossQty.add(safeStock).subtract(availableQty).subtract(onOrderQty);
            if (netQty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            PmMaterialPlan plan = loadSinglePlan(materialId);
            // 按批量 / 最小订购量向上取整，得到实际计划下单量
            BigDecimal plannedQty = roundUpToLot(netQty, plan);
            plannedByMaterial.put(materialId, plannedQty);

            // 用计划量展开子件需求
            PmBom bom = bomCache.get(materialId);
            if (bom == null) {
                continue;
            }
            BigDecimal baseQty = bom.getBaseQty() == null || bom.getBaseQty().compareTo(BigDecimal.ZERO) <= 0
                    ? BigDecimal.ONE : bom.getBaseQty();
            List<PmBomEntry> entries = entriesByBom.getOrDefault(bom.getId(), List.of());
            for (PmBomEntry bomEntry : entries) {
                BigDecimal lossRate = bomEntry.getLossRate() == null ? BigDecimal.ZERO : bomEntry.getLossRate();
                // 子件需求 = 父件计划量 ÷ BOM 基准数量 × 单位用量 × (1 + 损耗率)
                BigDecimal childDemand = plannedQty
                        .divide(baseQty, 10, RoundingMode.HALF_UP)
                        .multiply(bomEntry.getQty())
                        .multiply(BigDecimal.ONE.add(lossRate.divide(ONE_HUNDRED, 6, RoundingMode.HALF_UP)))
                        .setScale(SCALE, RoundingMode.HALF_UP);
                grossDemand.merge(bomEntry.getMaterialId(), childDemand, BigDecimal::add);
                if (bomEntry.getSubstituteFlag() != null && bomEntry.getSubstituteFlag() == 1
                        && bomEntry.getSubstituteMaterialId() != null) {
                    grossDemand.merge(bomEntry.getSubstituteMaterialId(), childDemand, BigDecimal::add);
                }
            }
        }

        // ===== 第二段：统一落库 =====
        // 毛需求已按层级汇总完毕，净需求已在各层计算完成，这里只负责生成结果记录
        Map<Long, PmMaterialPlan> planMap = loadMaterialPlans(new ArrayList<>(grossDemand.keySet()));
        Map<Long, MdMaterial> materialMap = loadMaterials(new ArrayList<>(grossDemand.keySet()));

        List<PmMrpResult> results = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : grossDemand.entrySet()) {
            Long materialId = entry.getKey();
            BigDecimal grossQty = entry.getValue();
            if (grossQty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            MdMaterial material = materialMap.get(materialId);
            if (material == null) {
                continue;
            }
            BigDecimal plannedQty = plannedByMaterial.get(materialId);
            if (plannedQty == null) {
                // 该物料净需求 <= 0，不需要补货
                continue;
            }
            PmMaterialPlan plan = planMap.get(materialId);
            BigDecimal availableQty = availableMap.getOrDefault(materialId, BigDecimal.ZERO);
            BigDecimal safeStock = material.getSafeStock() == null ? BigDecimal.ZERO : material.getSafeStock();
            BigDecimal onOrderQty = purchaseOnOrderMap.getOrDefault(materialId, BigDecimal.ZERO)
                    .add(productionOnOrderMap.getOrDefault(materialId, BigDecimal.ZERO));
            // 净需求还原为未取整的理论值，便于对比
            BigDecimal netQty = grossQty.add(safeStock).subtract(availableQty).subtract(onOrderQty);

            PmBom bom = bomCache.get(materialId);
            PmMrpResult result = new PmMrpResult();
            result.setRunNo(runNo);
            result.setMaterialId(materialId);
            result.setGrossQty(grossQty);
            result.setAvailableQty(availableQty);
            result.setOnOrderQty(onOrderQty);
            result.setSafeStock(safeStock);
            result.setNetQty(netQty.setScale(SCALE, RoundingMode.HALF_UP));
            // 净需求按批量取整后才是真正的计划下单量
            result.setPlannedQty(plannedQty);
            result.setReleaseStatus(PmMrpResult.RELEASE_NONE);

            int leadTimeDays = resolveLeadTime(plan, bom != null);
            result.setLeadTimeDays(leadTimeDays);
            // 建议下单日期 = 需求日 − 提前期
            result.setSuggestDate(demandDate.minusDays(leadTimeDays));

            if (bom != null) {
                result.setSuggestType(PmMrpResult.SUGGEST_PRODUCE);
                result.setBomId(bom.getId());
                result.setUnitCost(calcUnitCost(bom, planMap));
            } else {
                result.setSuggestType(PmMrpResult.SUGGEST_PURCHASE);
                result.setUnitCost(plan == null || plan.getCostPrice() == null
                        ? BigDecimal.ZERO : plan.getCostPrice());
            }
            results.add(result);
        }
        for (PmMrpResult result : results) {
            mrpResultMapper.insert(result);
        }
        return buildSuggestions(results, materialMap);
    }

    /**
     * 选出某物料在需求日可用的 BOM，并缓存结果。
     */
    private PmBom selectBom(Long materialId, LocalDate demandDate, Map<Long, PmBom> bomCache) {
        PmBom cached = bomCache.get(materialId);
        if (cached != null) {
            return cached;
        }
        PmBom bom = bomSelector.select(materialId, demandDate);
        if (bom != null) {
            bomCache.put(materialId, bom);
        }
        return bom;
    }

    /**
     * 载入单个物料的计划属性。
     */
    private PmMaterialPlan loadSinglePlan(Long materialId) {
        List<PmMaterialPlan> plans = materialPlanMapper.selectPlans(List.of(materialId));
        return plans.isEmpty() ? null : plans.get(0);
    }

    /**
     * 按批量（lotSize）或最小订购量（minOrderQty）向上取整。
     * <p>
     * 取两者中较大的批量倍数：既要满足最小起订量，也要满足批量整数倍。
     */
    private BigDecimal roundUpToLot(BigDecimal netQty, PmMaterialPlan plan) {
        BigDecimal lotSize = plan == null || plan.getLotSize() == null
                ? BigDecimal.ZERO : plan.getLotSize();
        BigDecimal minOrderQty = plan == null || plan.getMinOrderQty() == null
                ? BigDecimal.ZERO : plan.getMinOrderQty();
        BigDecimal step = lotSize.compareTo(minOrderQty) >= 0 ? lotSize : minOrderQty;
        if (step.compareTo(BigDecimal.ZERO) <= 0) {
            return netQty.setScale(SCALE, RoundingMode.HALF_UP);
        }
        BigDecimal steps = netQty.divide(step, 0, RoundingMode.CEILING);
        return steps.multiply(step).setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 提前期：自制件用生产提前期，采购件用采购提前期，都取不到时用通用提前期。
     */
    private int resolveLeadTime(PmMaterialPlan plan, boolean selfMade) {
        if (plan == null) {
            return 0;
        }
        if (selfMade) {
            Integer production = plan.getProductionLeadDays();
            if (production != null && production > 0) {
                return production;
            }
        } else {
            Integer purchase = plan.getPurchaseLeadDays();
            if (purchase != null && purchase > 0) {
                return purchase;
            }
        }
        Integer lead = plan.getLeadTimeDays();
        return lead == null || lead < 0 ? 0 : lead;
    }

    /**
     * 自制件单位成本：按 BOM 明细的标准用量 × 各子件成本价估算。
     */
    private BigDecimal calcUnitCost(PmBom bom, Map<Long, PmMaterialPlan> planMap) {
        List<PmBomEntry> entries = loadBomEntries().getOrDefault(bom.getId(), List.of());
        if (entries.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal baseQty = bom.getBaseQty() == null || bom.getBaseQty().compareTo(BigDecimal.ZERO) <= 0
                ? BigDecimal.ONE : bom.getBaseQty();
        BigDecimal total = BigDecimal.ZERO;
        for (PmBomEntry entry : entries) {
            PmMaterialPlan childPlan = planMap.get(entry.getMaterialId());
            BigDecimal costPrice = childPlan == null || childPlan.getCostPrice() == null
                    ? BigDecimal.ZERO : childPlan.getCostPrice();
            BigDecimal lossRate = entry.getLossRate() == null ? BigDecimal.ZERO : entry.getLossRate();
            total = total.add(entry.getQty()
                    .multiply(BigDecimal.ONE.add(lossRate.divide(ONE_HUNDRED, 6, RoundingMode.HALF_UP)))
                    .multiply(costPrice));
        }
        return total.divide(baseQty, SCALE, RoundingMode.HALF_UP);
    }

    // ------------------------------------------------------------
    // MRP 投放闭环
    // ------------------------------------------------------------

    /**
     * 投放采购订单：为「采购建议」的结果生成采购订单（pa_order / pa_order_entry）。
     * <p>
     * 直接写表而不改 pa 模块代码；单据状态置为「暂存」，
     * 由采购员在采购模块审核，避免绕过采购模块既有的审批与到货流程。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MrpReleaseResultVO releasePurchaseOrder(String runNo) {
        MrpReleaseResultVO result = new MrpReleaseResultVO();
        result.setRunNo(runNo);
        List<PmMrpResult> results = loadUnreleased(runNo, PmMrpResult.SUGGEST_PURCHASE);
        if (results.isEmpty()) {
            result.getMessages().add("该批次没有可投放的采购建议");
            return result;
        }
        Map<Long, PmMaterialPlan> planMap = loadMaterialPlans(results.stream()
                .map(PmMrpResult::getMaterialId)
                .distinct()
                .collect(Collectors.toList()));
        for (PmMrpResult mrp : results) {
            PmMaterialPlan plan = planMap.get(mrp.getMaterialId());
            if (plan == null) {
                skip(result, "物料不存在");
                continue;
            }
            Long supplierId = plan.getDefaultSupplierId();
            if (supplierId == null) {
                skip(result, "物料未设置默认供应商，无法自动投放");
                continue;
            }
            LocalDate orderDate = mrp.getSuggestDate() == null ? LocalDate.now() : mrp.getSuggestDate();
            PaOrder order = new PaOrder();
            order.setOrderNo(nextPaOrderNo(orderDate));
            order.setSupplierId(supplierId);
            order.setOrderDate(orderDate);
            // 交付日期 = 建议下单日 + 提前期
            order.setDeliveryDate(orderDate.plusDays(mrp.getLeadTimeDays() == null ? 0 : mrp.getLeadTimeDays()));
            order.setStatus(PaOrder.STATUS_DRAFT);
            order.setRemark("MRP 投放生成，批次：" + runNo);
            order.setTotalAmount(BigDecimal.ZERO);
            paOrderMapper.insert(order);

            BigDecimal qty = plannedQtyOf(mrp);
            BigDecimal price = mrp.getUnitCost() == null ? BigDecimal.ZERO : mrp.getUnitCost();
            BigDecimal taxRate = plan.getTaxRate() == null ? BigDecimal.ZERO : plan.getTaxRate();
            BigDecimal amount = qty.multiply(price)
                    .multiply(BigDecimal.ONE.add(taxRate.divide(ONE_HUNDRED, 6, RoundingMode.HALF_UP)))
                    .setScale(2, RoundingMode.HALF_UP);
            PaOrderEntry entry = new PaOrderEntry();
            entry.setOrderId(order.getId());
            entry.setMaterialId(mrp.getMaterialId());
            entry.setQty(qty);
            entry.setPrice(price);
            entry.setTaxRate(taxRate);
            entry.setAmount(amount);
            entry.setReceivedQty(BigDecimal.ZERO);
            entry.setRemark("MRP 投放生成");
            paOrderEntryMapper.insert(entry);

            PaOrder totalUpdate = new PaOrder();
            totalUpdate.setId(order.getId());
            totalUpdate.setTotalAmount(amount);
            paOrderMapper.updateById(totalUpdate);

            markReleased(mrp, order.getId());
            MrpReleasedOrderVO vo = new MrpReleasedOrderVO();
            vo.setMaterialId(mrp.getMaterialId());
            vo.setMaterialCode(plan.getMaterialCode());
            vo.setMaterialName(plan.getMaterialName());
            vo.setOrderNo(order.getOrderNo());
            vo.setOrderId(order.getId());
            vo.setOrderType(1);
            vo.setQty(qty);
            result.getOrders().add(vo);
            result.setReleasedCount(result.getReleasedCount() + 1);
        }
        return result;
    }

    /**
     * 投放生产订单：为「生产建议」的结果生成生产订单（pm_mo）。
     * <p>
     * 生成时设置 bom_id、数量、计划开工/完工日期；状态置为「暂存」由生产计划员审核。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MrpReleaseResultVO releaseProductionOrder(String runNo) {
        MrpReleaseResultVO result = new MrpReleaseResultVO();
        result.setRunNo(runNo);
        List<PmMrpResult> results = loadUnreleased(runNo, PmMrpResult.SUGGEST_PRODUCE);
        if (results.isEmpty()) {
            result.getMessages().add("该批次没有可投放的生产建议");
            return result;
        }
        Map<Long, PmMaterialPlan> planMap = loadMaterialPlans(results.stream()
                .map(PmMrpResult::getMaterialId)
                .distinct()
                .collect(Collectors.toList()));
        Long warehouseId = resolveFinishWarehouseId();
        for (PmMrpResult mrp : results) {
            PmMaterialPlan plan = planMap.get(mrp.getMaterialId());
            if (plan == null) {
                skip(result, "物料不存在");
                continue;
            }
            if (mrp.getBomId() == null) {
                skip(result, "未找到可用 BOM，无法生成生产订单");
                continue;
            }
            PmBom bom = bomMapper.selectById(mrp.getBomId());
            if (bom == null) {
                skip(result, "BOM 不存在，无法生成生产订单");
                continue;
            }
            LocalDate planStart = mrp.getSuggestDate() == null ? LocalDate.now() : mrp.getSuggestDate();
            int leadTime = mrp.getLeadTimeDays() == null ? 0 : mrp.getLeadTimeDays();
            LocalDate planEnd = planStart.plusDays(leadTime);

            PmMo mo = new PmMo();
            mo.setMoNo(nextMoNo(planStart));
            mo.setMaterialId(mrp.getMaterialId());
            mo.setBomId(bom.getId());
            mo.setQty(plannedQtyOf(mrp));
            mo.setProducedQty(BigDecimal.ZERO);
            mo.setWarehouseId(warehouseId);
            mo.setPlanStartDate(planStart);
            mo.setPlanEndDate(planEnd);
            mo.setStatus(PmMo.STATUS_DRAFT);
            mo.setLaborCost(BigDecimal.ZERO);
            mo.setOverheadCost(BigDecimal.ZERO);
            mo.setTotalCost(BigDecimal.ZERO);
            mo.setOutsourced(0);
            mo.setRemark("MRP 投放生成，批次：" + runNo);
            moMapper.insert(mo);

            markReleased(mrp, mo.getId());
            MrpReleasedOrderVO vo = new MrpReleasedOrderVO();
            vo.setMaterialId(mrp.getMaterialId());
            vo.setMaterialCode(plan.getMaterialCode());
            vo.setMaterialName(plan.getMaterialName());
            vo.setOrderNo(mo.getMoNo());
            vo.setOrderId(mo.getId());
            vo.setOrderType(2);
            vo.setQty(plannedQtyOf(mrp));
            result.getOrders().add(vo);
            result.setReleasedCount(result.getReleasedCount() + 1);
        }
        return result;
    }

    /**
     * 取成品仓库：优先取成品仓（wh_type = 1），没有则取第一个启用的仓库。
     */
    private Long resolveFinishWarehouseId() {
        List<MdWarehouse> warehouses = warehouseMapper.selectList(Wrappers.<MdWarehouse>lambdaQuery()
                .eq(MdWarehouse::getStatus, 0)
                .orderByAsc(MdWarehouse::getId));
        if (warehouses.isEmpty()) {
            return null;
        }
        return warehouses.stream()
                .filter(w -> w.getWhType() != null && w.getWhType() == 1)
                .map(MdWarehouse::getId)
                .findFirst()
                .orElse(warehouses.get(0).getId());
    }

    private void skip(MrpReleaseResultVO result, String reason) {
        result.setSkippedCount(result.getSkippedCount() + 1);
        result.getMessages().add(reason);
    }

    /**
     * 投放数量：优先用按批量取整后的计划数量；历史数据没有该字段时退回净需求。
     */
    private BigDecimal plannedQtyOf(PmMrpResult mrp) {
        BigDecimal planned = mrp.getPlannedQty();
        if (planned != null && planned.compareTo(BigDecimal.ZERO) > 0) {
            return planned;
        }
        return mrp.getNetQty() == null ? BigDecimal.ZERO : mrp.getNetQty();
    }

    private List<PmMrpResult> loadUnreleased(String runNo, int suggestType) {
        if (!StringUtils.hasText(runNo)) {
            throw new BizException("运算批次不能为空");
        }
        return mrpResultMapper.selectList(Wrappers.<PmMrpResult>lambdaQuery()
                .eq(PmMrpResult::getRunNo, runNo)
                .eq(PmMrpResult::getSuggestType, suggestType)
                .eq(PmMrpResult::getReleaseStatus, PmMrpResult.RELEASE_NONE)
                .orderByAsc(PmMrpResult::getId));
    }

    private void markReleased(PmMrpResult mrp, Long orderId) {
        PmMrpResult update = new PmMrpResult();
        update.setId(mrp.getId());
        update.setReleaseStatus(PmMrpResult.RELEASE_RELEASED);
        update.setReleasedOrderId(orderId);
        mrpResultMapper.updateById(update);
    }

    private String nextPaOrderNo(LocalDate date) {
        // 序号取自流水表原子自增：并发不撞号，软删除不重用旧号
        return serialNoGenerator.next("CG", date.format(MONTH_FORMATTER));
    }

    private String nextMoNo(LocalDate date) {
        // 序号取自流水表原子自增：并发不撞号，软删除不重用旧号
        return serialNoGenerator.next("MO", date.format(MONTH_FORMATTER));
    }

    @Override
    public PageResult<MrpSuggestionVO> pageResult(int pageNum, int pageSize, String runNo, Long materialId, Integer suggestType) {
        Page<PmMrpResult> page = mrpResultMapper.selectPage(new Page<>(pageNum, pageSize), Wrappers.<PmMrpResult>lambdaQuery()
                .eq(StringUtils.hasText(runNo), PmMrpResult::getRunNo, runNo)
                .eq(materialId != null, PmMrpResult::getMaterialId, materialId)
                .eq(suggestType != null, PmMrpResult::getSuggestType, suggestType)
                .orderByAsc(PmMrpResult::getId));
        List<Long> materialIds = page.getRecords().stream()
                .map(PmMrpResult::getMaterialId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, MdMaterial> materialMap = loadMaterials(materialIds);
        List<MrpSuggestionVO> records = buildSuggestions(page.getRecords(), materialMap);
        Page<MrpSuggestionVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return PageResult.of(voPage);
    }

    @Override
    public String latestRunNo() {
        PmMrpResult latest = mrpResultMapper.selectOne(Wrappers.<PmMrpResult>lambdaQuery()
                .orderByDesc(PmMrpResult::getId)
                .last("limit 1"));
        return latest == null ? null : latest.getRunNo();
    }

    private void collectSalesDemand(Map<Long, BigDecimal> demand) {
        List<SaOrder> orders = saOrderMapper.selectList(Wrappers.<SaOrder>lambdaQuery()
                .eq(SaOrder::getStatus, SaOrder.STATUS_AUDITED));
        if (orders.isEmpty()) {
            return;
        }
        List<Long> orderIds = orders.stream().map(SaOrder::getId).collect(Collectors.toList());
        List<SaOrderEntry> entries = saOrderEntryMapper.selectList(Wrappers.<SaOrderEntry>lambdaQuery()
                .in(SaOrderEntry::getOrderId, orderIds));
        for (SaOrderEntry entry : entries) {
            BigDecimal delivered = entry.getDeliveredQty() == null ? BigDecimal.ZERO : entry.getDeliveredQty();
            BigDecimal remaining = entry.getQty().subtract(delivered);
            if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                demand.merge(entry.getMaterialId(), remaining, BigDecimal::add);
            }
        }
    }

    private Map<Long, BigDecimal> collectPurchaseOnOrder() {
        List<PaOrder> orders = paOrderMapper.selectList(Wrappers.<PaOrder>lambdaQuery()
                .eq(PaOrder::getStatus, PaOrder.STATUS_AUDITED));
        if (orders.isEmpty()) {
            return new HashMap<>();
        }
        List<Long> orderIds = orders.stream().map(PaOrder::getId).collect(Collectors.toList());
        List<PaOrderEntry> entries = paOrderEntryMapper.selectList(Wrappers.<PaOrderEntry>lambdaQuery()
                .in(PaOrderEntry::getOrderId, orderIds));
        Map<Long, BigDecimal> map = new HashMap<>();
        for (PaOrderEntry entry : entries) {
            BigDecimal received = entry.getReceivedQty() == null ? BigDecimal.ZERO : entry.getReceivedQty();
            BigDecimal remaining = entry.getQty().subtract(received);
            if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                map.merge(entry.getMaterialId(), remaining, BigDecimal::add);
            }
        }
        return map;
    }

    private Map<Long, BigDecimal> collectProductionOnOrder() {
        List<PmMo> mos = moMapper.selectList(Wrappers.<PmMo>lambdaQuery()
                .in(PmMo::getStatus, PmMo.STATUS_AUDITED, PmMo.STATUS_PROCESSING));
        Map<Long, BigDecimal> map = new HashMap<>();
        for (PmMo mo : mos) {
            BigDecimal produced = mo.getProducedQty() == null ? BigDecimal.ZERO : mo.getProducedQty();
            BigDecimal remaining = mo.getQty().subtract(produced);
            if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                map.merge(mo.getMaterialId(), remaining, BigDecimal::add);
            }
        }
        return map;
    }

    private Map<Long, BigDecimal> collectAvailableStock() {
        List<Long> materialIds = materialMapper.selectList(Wrappers.<MdMaterial>lambdaQuery())
                .stream().map(MdMaterial::getId).collect(Collectors.toList());
        if (materialIds.isEmpty()) {
            return new HashMap<>();
        }
        List<Map<String, Object>> rows = stockMapper.sumAvailableByMaterialIds(materialIds);
        Map<Long, BigDecimal> map = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object materialId = row.get("materialId");
            Object value = row.get("availableQty");
            if (materialId != null && value != null) {
                map.put(((Number) materialId).longValue(), new BigDecimal(String.valueOf(value)));
            }
        }
        return map;
    }

    /**
     * 一次性载入全部 BOM 明细，避免展开过程中反复查库。
     */
    private Map<Long, List<PmBomEntry>> loadBomEntries() {
        List<PmBomEntry> entries = bomEntryMapper.selectList(null);
        return entries.stream().collect(Collectors.groupingBy(PmBomEntry::getBomId));
    }

    /**
     * 构建 BOM 拓扑序：父件排在子件之前。
     * <p>
     * 多级 BOM 中同一物料可能既是别的物料的子件、自己又有子件，也可能被多个父件
     * 同时引用。必须按拓扑序处理，保证处理到某物料时所有父件的贡献都已累加完毕，
     * 否则后到达的需求贡献会被丢弃。
     * <p>
     * 用 Kahn 算法：入度为 0 的物料（没有父件）先出队。若存在环（数据配置错误导致
     * BOM 互相引用），剩余节点按物料 ID 顺序追加，保证算法不会死循环。
     */
    private List<Long> buildTopoOrder(Map<Long, List<PmBomEntry>> entriesByBom,
                                      Map<Long, PmBom> bomCache,
                                      LocalDate demandDate) {
        // 建立「父物料 → 子物料」的有向边
        Map<Long, Set<Long>> childrenMap = new LinkedHashMap<>();
        Map<Long, Integer> inDegree = new LinkedHashMap<>();

        for (Map.Entry<Long, List<PmBomEntry>> item : entriesByBom.entrySet()) {
            Long bomId = item.getKey();
            PmBom bom = bomMapper.selectById(bomId);
            if (bom == null || bom.getMaterialId() == null) {
                continue;
            }
            Long parentId = bom.getMaterialId();
            // 预热 BOM 缓存，避免拓扑排序时重复查库选版本
            if (!bomCache.containsKey(parentId)) {
                selectBom(parentId, demandDate, bomCache);
            }
            inDegree.putIfAbsent(parentId, 0);
            for (PmBomEntry bomEntry : item.getValue()) {
                Long childId = bomEntry.getMaterialId();
                if (childId == null || childId.equals(parentId)) {
                    continue;
                }
                childrenMap.computeIfAbsent(parentId, k -> new LinkedHashSet<>()).add(childId);
                inDegree.merge(childId, 1, Integer::sum);
                inDegree.putIfAbsent(childId, 0);
            }
        }

        // Kahn 拓扑排序：入度为 0 的先出队
        List<Long> ordered = new ArrayList<>();
        Deque<Long> queue = new ArrayDeque<>();
        inDegree.forEach((materialId, degree) -> {
            if (degree == 0) {
                queue.add(materialId);
            }
        });

        Map<Long, Integer> remaining = new HashMap<>(inDegree);
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            ordered.add(current);
            for (Long childId : childrenMap.getOrDefault(current, Set.of())) {
                int degree = remaining.merge(childId, -1, Integer::sum);
                if (degree == 0) {
                    queue.add(childId);
                }
            }
        }

        // 存在环时按 ID 追加剩余节点，避免死循环
        if (ordered.size() < remaining.size()) {
            List<Long> rest = remaining.keySet().stream()
                    .filter(id -> !ordered.contains(id))
                    .sorted()
                    .collect(Collectors.toList());
            ordered.addAll(rest);
        }
        return ordered;
    }

    private Map<Long, PmMaterialPlan> loadMaterialPlans(List<Long> materialIds) {
        if (materialIds == null || materialIds.isEmpty()) {
            return new HashMap<>();
        }
        return materialPlanMapper.selectPlans(materialIds).stream()
                .collect(Collectors.toMap(PmMaterialPlan::getId, Function.identity(), (a, b) -> a));
    }

    private Map<Long, MdMaterial> loadMaterials(List<Long> materialIds) {
        if (materialIds == null || materialIds.isEmpty()) {
            return new HashMap<>();
        }
        return materialMapper.selectList(Wrappers.<MdMaterial>lambdaQuery().in(MdMaterial::getId, materialIds))
                .stream()
                .collect(Collectors.toMap(MdMaterial::getId, Function.identity()));
    }

    private List<MrpSuggestionVO> buildSuggestions(List<PmMrpResult> results, Map<Long, MdMaterial> materialMap) {
        return results.stream().map(result -> {
            MrpSuggestionVO vo = new MrpSuggestionVO();
            vo.setId(result.getId());
            vo.setRunNo(result.getRunNo());
            vo.setMaterialId(result.getMaterialId());
            vo.setSuggestType(result.getSuggestType());
            vo.setGrossQty(result.getGrossQty());
            vo.setAvailableQty(result.getAvailableQty());
            vo.setOnOrderQty(result.getOnOrderQty());
            vo.setSafeStock(result.getSafeStock());
            vo.setNetQty(result.getNetQty());
            vo.setPlannedQty(result.getPlannedQty());
            vo.setBomId(result.getBomId());
            vo.setSuggestDate(result.getSuggestDate());
            vo.setLeadTimeDays(result.getLeadTimeDays());
            vo.setUnitCost(result.getUnitCost());
            vo.setReleaseStatus(result.getReleaseStatus());
            vo.setReleasedOrderId(result.getReleasedOrderId());
            MdMaterial material = materialMap.get(result.getMaterialId());
            if (material != null) {
                vo.setMaterialCode(material.getMaterialCode());
                vo.setMaterialName(material.getMaterialName());
                vo.setSpec(material.getSpec());
                vo.setUnit(material.getUnit());
            }
            if (result.getBomId() != null) {
                PmBom bom = bomMapper.selectById(result.getBomId());
                if (bom != null) {
                    vo.setBomNo(bom.getBomNo());
                }
            }
            return vo;
        }).collect(Collectors.toList());
    }

    private String nextRunNo() {
        // 序号取自流水表原子自增：并发不撞号，软删除不重用旧号
        return serialNoGenerator.next("MRP", LocalDate.now().format(DATE_FORMATTER));
    }

}








