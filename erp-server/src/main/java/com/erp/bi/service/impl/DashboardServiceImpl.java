package com.erp.bi.service.impl;

import com.erp.bi.domain.vo.DashboardVO;
import com.erp.bi.mapper.DashboardMapper;
import com.erp.bi.service.DashboardService;
import com.erp.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作台统计服务。所有指标来自真实业务数据聚合。
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final int TOP_LIMIT = 8;

    private final DashboardMapper dashboardMapper;

    @Override
    public DashboardVO getDashboard() {
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);
        String startDate = currentMonth.atDay(1).toString();
        String endDate = currentMonth.atEndOfMonth().toString();

        DashboardVO vo = new DashboardVO();
        vo.setPeriodLabel(currentMonth.getYear() + " 年 " + currentMonth.getMonthValue() + " 月");
        vo.setSalesAmount(dashboardMapper.sumSalesAmount(startDate, endDate));
        vo.setPurchaseAmount(dashboardMapper.sumPurchaseAmount(startDate, endDate));
        vo.setStockAmount(dashboardMapper.sumStockAmount());
        vo.setReceivableAmount(dashboardMapper.sumReceivableAmount());
        vo.setPayableAmount(dashboardMapper.sumPayableAmount());

        vo.setPendingAuditCount(dashboardMapper.countPendingAudit());
        vo.setStockWarningCount(dashboardMapper.countStockWarning());
        vo.setOverdueReceivableCount(dashboardMapper.countOverdueReceivable(today.toString()));
        vo.setWipMoCount(dashboardMapper.countWipMo());

        Long userId = SecurityUtils.getCurrentUserId();
        vo.setTodoTaskCount(userId == null ? 0 : dashboardMapper.countTodoTasks(userId));

        vo.setSalesTrend(buildSalesTrend(today.getYear()));
        vo.setStockTop(buildStockTop());
        vo.setOverdueTop(buildOverdueTop(today));
        vo.setTodoItems(buildTodoItems(vo));
        return vo;
    }

    /**
     * 构建销售趋势。补齐没有数据的月份，保证曲线连续。
     */
    private List<DashboardVO.TrendPointVO> buildSalesTrend(int year) {
        Map<String, BigDecimal> raw = new LinkedHashMap<>();
        for (Map<String, Object> row : dashboardMapper.salesTrendByMonth(year)) {
            Object label = row.get("label");
            Object amount = row.get("amount");
            if (label != null) {
                raw.put(String.valueOf(label), toDecimal(amount));
            }
        }
        List<DashboardVO.TrendPointVO> points = new ArrayList<>(12);
        for (int month = 1; month <= 12; month++) {
            String label = year + "-" + String.format("%02d", month);
            DashboardVO.TrendPointVO point = new DashboardVO.TrendPointVO();
            point.setLabel(month + "月");
            point.setAmount(raw.getOrDefault(label, BigDecimal.ZERO));
            points.add(point);
        }
        return points;
    }

    private List<DashboardVO.RankItemVO> buildStockTop() {
        List<DashboardVO.RankItemVO> result = new ArrayList<>();
        for (Map<String, Object> row : dashboardMapper.stockTop(TOP_LIMIT)) {
            DashboardVO.RankItemVO item = new DashboardVO.RankItemVO();
            item.setName(stringOf(row.get("name")));
            item.setAmount(toDecimal(row.get("amount")));
            item.setExtra(stringOf(row.get("extra")));
            result.add(item);
        }
        return result;
    }

    private List<DashboardVO.RankItemVO> buildOverdueTop(LocalDate asOfDate) {
        List<DashboardVO.RankItemVO> result = new ArrayList<>();
        for (Map<String, Object> row : dashboardMapper.overdueTop(asOfDate.toString(), TOP_LIMIT)) {
            DashboardVO.RankItemVO item = new DashboardVO.RankItemVO();
            item.setName(stringOf(row.get("name")));
            item.setAmount(toDecimal(row.get("amount")));
            item.setExtra(stringOf(row.get("extra")));
            result.add(item);
        }
        return result;
    }

    /**
     * 构建待办事项列表，按数量倒序展示需要用户关注的业务。
     */
    private List<DashboardVO.TodoItemVO> buildTodoItems(DashboardVO vo) {
        List<DashboardVO.TodoItemVO> items = new ArrayList<>();
        items.add(todo("待审核单据", "AUDIT", vo.getPendingAuditCount(), "/pa/instock"));
        items.add(todo("我的待办任务", "TASK", vo.getTodoTaskCount(), "/wf/todo"));
        items.add(todo("库存预警", "WARNING", vo.getStockWarningCount(), "/ic/stock"));
        items.add(todo("逾期应收", "OVERDUE", vo.getOverdueReceivableCount(), "/ar/aging"));
        items.add(todo("在制工单", "PRODUCTION", vo.getWipMoCount(), "/pm/mo"));
        items.removeIf(item -> item.getCount() == null || item.getCount() == 0);
        return items;
    }

    private DashboardVO.TodoItemVO todo(String title, String type, Integer count, String route) {
        DashboardVO.TodoItemVO item = new DashboardVO.TodoItemVO();
        item.setTitle(title);
        item.setType(type);
        item.setCount(count == null ? 0 : count);
        item.setRoute(route);
        return item;
    }

    private BigDecimal toDecimal(Object value) {
        return value == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(value));
    }

    private String stringOf(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}








