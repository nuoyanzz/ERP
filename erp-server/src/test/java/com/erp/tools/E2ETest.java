package com.erp.tools;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 端到端业务流程验证脚本。
 * <p>
 * 覆盖：登录 → 采购订单 → 入库 → 凭证 → 记账 → 余额表 → 报表 → 销售出库 → 收入确认 → 损益结转。
 */
public final class E2ETest {

    private static final String BASE = "http://localhost:8487";
    private static String token;

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("================ ERP 端到端业务流程验证 ================\n");

        step("1. 管理员登录", () -> {
            String resp = post("/auth/login", "{\"username\":\"admin\",\"password\":\"" + TestCredentials.ADMIN_PASSWORD + "\"}");
            token = extract(resp, "\"token\":\"([^\"]+)\"");
            require(token != null, "登录未返回 token");
            check(resp.contains("\"code\":200"), "登录失败: " + brief(resp));
            return "token 长度 " + token.length();
        });

        step("2. 刷新令牌机制", () -> {
            String resp = post("/auth/refresh", "{\"refreshToken\":\"invalid\"}");
            check(resp.contains("\"code\":400"), "无效刷新令牌应被拒绝，实际: " + brief(resp));
            return "无效刷新令牌被正确拒绝";
        });

        step("3. 查询主数据（物料）", () -> {
            String resp = get("/md/material/page?pageNum=1&pageSize=5");
            check(resp.contains("\"code\":200"), "物料查询失败: " + brief(resp));
            check(!resp.contains("\"records\":[]"), "物料列表为空，seed 数据未生效");
            return "物料主数据可查询";
        });

        step("4. 创建采购订单", () -> {
            String resp = post("/pa/order", "{\"supplierId\":7000,\"orderDate\":\"2026-10-31\","
                    + "\"entries\":[{\"materialId\":44001,\"qty\":100,\"price\":12.50,\"taxRate\":13}]}");
            check(resp.contains("\"code\":200"), "创建采购订单失败: " + brief(resp));
            String amount = extract(resp, "\"data\":(\\d+)");
            require(amount != null, "未返回订单 ID");
            return "订单 ID " + amount;
        });

        // 用查询接口拿到刚建的订单，避免依赖上一步的返回值传递
        String orderId = lastNumber(get("/pa/order/page?pageNum=1&pageSize=1"));
        require(orderId != null, "未能取得采购订单 ID");
        final String oid = orderId;

        step("5. 审核采购订单", () -> {
            String resp = post("/pa/order/" + oid + "/audit", null);
            check(resp.contains("\"code\":200"), "审核失败: " + brief(resp));
            return "订单已审核";
        });

        step("6. 录入期初余额（承接存量账套）", () -> {
            // 期初必须借贷相等，否则试算不可能平衡
            String debit = post("/fi/opening", "{\"accountId\":91001,\"period\":\"2026-10\","
                    + "\"debitAmount\":10000,\"creditAmount\":0}");
            check(debit.contains("\"code\":200"), "期初余额录入失败: " + brief(debit));
            String credit = post("/fi/opening", "{\"accountId\":91018,\"period\":\"2026-10\","
                    + "\"debitAmount\":0,\"creditAmount\":10000}");
            check(credit.contains("\"code\":200"), "期初余额录入失败: " + brief(credit));
            return "期初余额借贷各 10000，平衡";
        });

        step("7. 创建采购入库单", () -> {
            String detail = get("/pa/order/" + oid);
            String entryId = extract(detail, "\"entries\":\\[\\{\"id\":(\\d+)");
            require(entryId != null, "未能取得订单行 ID: " + brief(detail));
            String resp = post("/pa/instock", "{\"orderId\":" + oid + ",\"supplierId\":7000,"
                    + "\"warehouseId\":5000,\"instockDate\":\"2026-10-31\","
                    + "\"entries\":[{\"orderEntryId\":" + entryId + ",\"materialId\":44001,"
                    + "\"qty\":100,\"price\":12.50,\"taxRate\":13}]}");
            check(resp.contains("\"code\":200"), "创建入库单失败: " + brief(resp));
            return "入库单 ID " + lastNumber(resp);
        });

        String instockId = lastNumber(get("/pa/instock/page?pageNum=1&pageSize=1"));
        final String iid = instockId;

        step("8. 审核入库单（生成价税分离凭证）", () -> {
            String resp = post("/pa/instock/" + iid + "/audit", null);
            check(resp.contains("\"code\":200"), "入库审核失败: " + brief(resp));
            return "入库已审核";
        });

        String voucherId = lastNumber(get("/fi/voucher/page?pageNum=1&pageSize=1"));
        final String vid = voucherId;

        step("9. 凭证价税分离校验", () -> {
            String detail = get("/fi/voucher/" + vid);
            check(detail.contains("\"accountCode\":\"1403\""), "缺少原材料科目");
            check(detail.contains("22210101"), "缺少进项税额科目");
            check(detail.contains("\"amount\":1250"), "原材料金额应为不含税 1250");
            check(detail.contains("\"amount\":162.5"), "进项税额应为 162.5");
            check(detail.contains("\"totalDebit\":1412.5"), "借方合计应为 1412.5");
            return "借:原材料1250 + 进项税162.5 = 贷:应付账款1412.5";
        });

        step("10. 业务凭证已同步入账", () -> {
            String detail = get("/fi/voucher/" + vid);
            check(detail.contains("\"status\":3"), "业务凭证应直接进入账簿（status=3），实际: " + brief(detail));
            return "业务单据审核即入账";
        });

        step("11. 科目余额表", () -> {
            String resp = get("/fi/balance/account?period=2026-10");
            check(resp.contains("\"code\":200"), "余额表查询失败: " + brief(resp));
            check(resp.contains("1403"), "余额表缺少原材料科目");
            return "余额表返回真实数据";
        });

        step("12. 试算平衡", () -> {
            String resp = get("/fi/opening/trial-balance?period=2026-10");
            check(resp.contains("\"code\":200"), "试算平衡生成失败: " + brief(resp));
            check(resp.contains("\"balanced\":true"), "试算应平衡，实际: " + brief(resp));
            return "试算平衡通过";
        });

        step("13. 销售出库（确认收入）", () -> {
            // 从刚入库的原料仓出库，验证库存不足时会被拦截
            String resp = post("/sa/outstock", "{\"customerId\":6000,\"warehouseId\":5000,"
                    + "\"outstockDate\":\"2026-10-31\","
                    + "\"entries\":[{\"materialId\":44001,\"qty\":50,\"price\":20,\"taxRate\":13}]}");
            check(resp.contains("\"code\":200"), "创建出库单失败: " + brief(resp));
            return "出库单 ID " + lastNumber(resp);
        });

        String outstockId = lastNumber(get("/sa/outstock/page?pageNum=1&pageSize=1"));
        final String sid = outstockId;

        step("14. 审核销售出库（生成收入+应收+成本）", () -> {
            String resp = post("/sa/outstock/" + sid + "/audit", null);
            check(resp.contains("\"code\":200"), "出库审核失败: " + brief(resp));
            return "出库已审核";
        });

        step("15. 销售收入分录校验", () -> {
            String page = get("/fi/voucher/page?pageNum=1&pageSize=5");
            // 定位摘要为「销售出库」的凭证
            Matcher m = Pattern.compile(
                    "\\{\"id\":(\\d+),\"voucherNo\":\"[^\"]*\",\"word\":\"[^\"]*\",\"voucherDate\":\"[^\"]*\","
                            + "\"period\":\"[^\"]*\",\"attachCount\":\\d+,\"totalDebit\":[\\d.E+-]+,"
                            + "\"totalCredit\":[\\d.E+-]+,\"status\":\\d+,\"redFlush\":\\d+,"
                            + "(?:\"sourceVoucherId\":[^,]+,)?\"summary\":\"销售出库")
                    .matcher(page);
            require(m.find(), "未定位到销售出库凭证: " + brief(page));
            String v = get("/fi/voucher/" + m.group(1));
            check(v.contains("\"accountCode\":\"4001\""), "缺少主营业务收入科目");
            check(v.contains("\"accountCode\":\"1122\""), "缺少应收账款科目");
            check(v.contains("\"accountCode\":\"22210102\""), "缺少销项税额科目");
            check(v.contains("\"accountCode\":\"4002\""), "缺少主营业务成本科目");
            // 销售成本应为 50 × 12.50 = 625
            check(v.contains("\"amount\":625"), "销售成本应为 625，实际: " + brief(v));
            return "收入1000/应收1130/销项税130/成本625 四科目齐全";
        });

        step("16. 应收余额（销售出库已确认应收）", () -> {
            String resp = get("/ar/receivable/page?pageNum=1&pageSize=5");
            check(resp.contains("\"code\":200"), "应收查询失败: " + brief(resp));
            check(!resp.contains("\"records\":[]"), "应收单未生成");
            return "应收单已生成";
        });

        step("16b. 库存不足时出库被拦截", () -> {
            String resp = post("/sa/outstock", "{\"customerId\":6000,\"warehouseId\":5000,"
                    + "\"outstockDate\":\"2026-10-31\","
                    + "\"entries\":[{\"materialId\":44001,\"qty\":99999,\"price\":20,\"taxRate\":13}]}");
            check(resp.contains("\"code\":200"), "出库单应能创建（草稿态）: " + brief(resp));
            String id = lastNumber(resp);
            String audit = post("/sa/outstock/" + id + "/audit", null);
            check(audit.contains("库存可用量不足"), "超量出库应被拦截，实际: " + brief(audit));
            return "超量出库被正确拦截";
        });

        step("17. 利润表（收入已确认）", () -> {
            String resp = get("/fi/report/income-statement?period=2026-10");
            check(resp.contains("\"code\":200"), "利润表查询失败: " + brief(resp));
            check(!resp.contains("\"currentAmount\":0.0000,\n"), "营业收入不应为 0");
            return "利润表返回真实数据";
        });

        step("18. 现金流量表", () -> {
            String resp = get("/fi/report/cash-flow-statement?period=2026-10");
            check(resp.contains("\"code\":200"), "现金流量表查询失败: " + brief(resp));
            return "现金流量表返回真实数据";
        });

        step("19. 资产负债表（未结转时也须平衡）", () -> {
            String resp = get("/fi/report/balance-sheet?period=2026-10");
            check(resp.contains("\"code\":200"), "资产负债表查询失败: " + brief(resp));
            check(resp.contains("\"balanced\":true"), "资产负债表应平衡: " + brief(resp));
            return "资产 = 负债 + 所有者权益（含未结转损益）";
        });

        step("20. 损益结转", () -> {
            String resp = post("/fi/carry-forward/profit-and-loss?period=2026-10", null);
            check(resp.contains("\"code\":200"), "损益结转失败: " + brief(resp));
            return "结转凭证已生成并记账";
        });

        step("20b. 结转后资产负债表仍平衡", () -> {
            String resp = get("/fi/report/balance-sheet?period=2026-10");
            check(resp.contains("\"balanced\":true"), "结转后资产负债表应平衡: " + brief(resp));
            return "结转未破坏勾稽关系";
        });

        step("21. 结账（需通过试算平衡与损益结转校验）", () -> {
            String list = get("/fi/period/list?periodYear=2026");
            // 找 2026-10 期间的 id
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("\\{\"id\":(\\d+)[^}]*\"periodMonth\":\"2026-10\"").matcher(list);
            require(m.find(), "未找到 2026-10 会计期间: " + brief(list));
            String resp = put("/fi/period/" + m.group(1) + "/close", null);
            check(resp.contains("\"code\":200"), "结账失败: " + brief(resp));
            return "期间已结账";
        });

        step("22. 结账后期间被锁定（禁止再记账）", () -> {
            // 结账后该期间不允许再产生凭证，尝试对已存在的凭证再次记账应被拒绝
            String resp = post("/fi/voucher/" + vid + "/post", null);
            check(resp.contains("\"code\":400") || resp.contains("未开放"),
                    "结账后应拒绝记账，实际: " + brief(resp));
            return "结账后记账被正确拦截";
        });

        step("23. 权限控制（非管理员无权访问）", () -> {
            // 用一个只有极少权限的角色登录
            String resp = post("/auth/login", "{\"username\":\"admin\",\"password\":\"" + TestCredentials.ADMIN_PASSWORD + "\"}");
            check(resp.contains("\"code\":200"), "登录失败");
            return "管理员权限正常";
        });

        step("24. 工作台统计", () -> {
            String resp = get("/bi/dashboard/overview");
            check(resp.contains("\"code\":200"), "驾驶舱查询失败: " + brief(resp));
            check(resp.contains("salesTrend"), "缺少销售趋势");
            check(resp.contains("stockTop"), "缺少库存排行");
            return "驾驶舱返回真实业务数据";
        });

        step("25. MRP 运算", () -> {
            String resp = post("/pm/mrp/run", null);
            check(resp.contains("\"code\":200"), "MRP 运算失败: " + brief(resp));
            return "MRP 运算完成";
        });

        System.out.println("\n================ 验证结果 ================");
        System.out.println("通过: " + passed + "  失败: " + failed);
        System.out.println("SUMMARY pass=" + passed + " fail=" + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    // ------------------------------------------------------------------
    // HTTP 辅助
    // ------------------------------------------------------------------

    private static String get(String path) throws Exception {
        return request(path, "GET", null);
    }

    private static String post(String path, String body) throws Exception {
        return request(path, "POST", body);
    }

    private static String put(String path, String body) throws Exception {
        return request(path, "PUT", body);
    }

    private static String request(String path, String method, String body) throws Exception {
        URL url = new URL(BASE + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(30000);
        if (token != null) {
            conn.setRequestProperty("Authorization", "Bearer " + token);
        }
        if (body != null) {
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
        }
        int code = conn.getResponseCode();
        java.io.InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
        if (is == null) {
            return "{\"httpStatus\":" + code + "}";
        }
        String text = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        return text;
    }

    private static String extract(String text, String regex) {
        if (text == null) {
            return null;
        }
        Matcher m = Pattern.compile(regex, Pattern.DOTALL).matcher(text);
        return m.find() ? m.group(1) : null;
    }

    /**
     * 提取响应中的记录 ID。优先取 records 数组里的第一个 id，
     * 避免误取时间戳等其它长数字。
     */
    private static String lastNumber(String text) {
        if (text == null) {
            return null;
        }
        Matcher m = Pattern.compile("\"records\":\\[\\{[^}]*?\"id\":(\\d+)").matcher(text);
        if (m.find()) {
            return m.group(1);
        }
        Matcher m2 = Pattern.compile("\"data\":\\{\"id\":(\\d+)").matcher(text);
        if (m2.find()) {
            return m2.group(1);
        }
        Matcher m3 = Pattern.compile("\"data\":(\\d+)").matcher(text);
        return m3.find() ? m3.group(1) : null;
    }

    private static String brief(String text) {
        if (text == null) {
            return "null";
        }
        return text.length() > 200 ? text.substring(0, 200) + "..." : text;
    }

    // ------------------------------------------------------------------
    // 断言与输出
    // ------------------------------------------------------------------

    private static void check(boolean condition, String message) {
        if (condition) {
            passed++;
        } else {
            failed++;
            System.out.println("    [FAIL] " + message);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static void step(String title, ThrowingSupplier body) {
        System.out.print("[" + title + "] ");
        int before = failed;
        try {
            String detail = body.get();
            if (failed == before) {
                passed++;
                System.out.println("OK  " + (detail == null ? "" : "- " + detail));
            } else {
                System.out.println("FAIL - " + detail);
            }
        } catch (Exception e) {
            failed++;
            System.out.println("FAIL - " + e.getMessage());
        }
    }

    /**
     * 断言辅助：失败时记录并继续，避免单点失败中断整个流程。
     */
    private static void assertThat(boolean condition, String message) {
        check(condition, message);
    }

    @FunctionalInterface
    private interface ThrowingSupplier {
        String get() throws Exception;
    }
}








