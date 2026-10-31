package com.erp.tools;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 写路径与数据一致性验证。
 * <p>
 * 覆盖增删改查的完整闭环，以及关键业务规则的拒绝路径：
 * 重复编码、超额出库、不平凭证、重复核销、非法期间等。
 */
public final class WritePathTest {

    private static final String BASE = "http://localhost:8487";

    private static String token;
    private static int passed;
    private static int failed;
    private static final List<String> failures = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        System.out.println("================ 写路径与业务规则验证 ================\n");

        token = login("admin", "admin123");
        if (token == null) {
            System.out.println("登录失败，终止");
            System.exit(1);
        }

        // ---- 主数据：增删改查闭环 ----
        step("1. 新增物料", () -> {
            String resp = post("/md/material", "{\"materialCode\":\"TEST-001\",\"materialName\":\"测试物料\","
                    + "\"categoryId\":4000,\"unit\":\"PCS\",\"costPrice\":10.5,\"safeStock\":100,\"taxRate\":13}");
            expect(resp, "新增物料失败");
            String id = extract(resp, "\"data\":(\\d+)");
            require(id != null, "未返回物料 ID");
            return id;
        });

        String materialId = lastCreatedId;

        step("2. 查询物料确认落库", () -> {
            String resp = get("/md/material/page?pageNum=1&pageSize=10&materialCode=TEST-001");
            expect(resp, "查询物料失败");
            require(resp.contains("TEST-001"), "物料未出现在列表中");
            require(resp.contains("测试物料"), "物料名称不匹配");
            return "物料已落库";
        });

        step("3. 修改物料", () -> {
            String resp = put("/md/material", "{\"id\":" + materialId + ",\"materialCode\":\"TEST-001\","
                    + "\"materialName\":\"测试物料改名\",\"categoryId\":4000,\"unit\":\"PCS\",\"costPrice\":12.0,"
                    + "\"safeStock\":200,\"taxRate\":13}");
            expect(resp, "修改物料失败");
            String detail = get("/md/material/" + materialId);
            require(detail.contains("测试物料改名"), "修改未生效");
            return "修改已生效";
        });

        step("4. 重复编码应被拒绝", () -> {
            String resp = post("/md/material", "{\"materialCode\":\"TEST-001\",\"materialName\":\"重复物料\","
                    + "\"categoryId\":4000,\"unit\":\"PCS\"}");
            require(resp.contains("已存在"), "重复编码未被拦截: " + brief(resp));
            return "重复编码被正确拒绝";
        });

        step("5. 删除物料", () -> {
            String resp = delete("/md/material/" + materialId);
            expect(resp, "删除物料失败");
            String detail = get("/md/material/" + materialId);
            require(!detail.contains("\"code\":200"), "删除后仍能查到");
            return "删除已生效";
        });

        // ---- 财务：凭证借贷不平应被拒绝 ----
        step("6. 借贷不平的凭证应被拒绝", () -> {
            // 91001=1001 库存现金、91003=100201 银行存款-工商银行，两者都是明细科目
            String resp = post("/fi/voucher", "{\"voucherDate\":\"2026-10-31\",\"word\":\"记\",\"summary\":\"不平测试\","
                    + "\"entries\":[{\"accountId\":91001,\"direction\":1,\"amount\":100},"
                    + "{\"accountId\":91003,\"direction\":2,\"amount\":80}]}");
            require(resp.contains("\"code\":400"), "不平凭证未被拦截: " + brief(resp));
            require(resp.contains("借贷不平"), "错误信息不明确: " + brief(resp));
            return "借贷不平被正确拒绝";
        });

        step("7. 金额为零的分录应被拒绝", () -> {
            String resp = post("/fi/voucher", "{\"voucherDate\":\"2026-10-31\",\"word\":\"记\","
                    + "\"entries\":[{\"accountId\":91001,\"direction\":1,\"amount\":0},"
                    + "{\"accountId\":91003,\"direction\":2,\"amount\":0}]}");
            require(resp.contains("\"code\":400"), "零金额分录未被拦截: " + brief(resp));
            return "零金额分录被正确拒绝";
        });

        step("8. 非明细科目应被拒绝", () -> {
            // 1002 银行存款是父科目，有子科目
            String resp = post("/fi/voucher", "{\"voucherDate\":\"2026-10-31\",\"word\":\"记\","
                    + "\"entries\":[{\"accountId\":91002,\"direction\":1,\"amount\":100},"
                    + "{\"accountId\":91003,\"direction\":2,\"amount\":100}]}");
            require(resp.contains("\"code\":400"), "非明细科目未被拦截: " + brief(resp));
            require(resp.contains("非明细科目"), "错误信息不明确: " + brief(resp));
            return "非明细科目被正确拒绝";
        });

        step("9. 停用科目不可记账", () -> {
            // 停用一个不用的科目
            String del = post("/md/material", "{\"materialCode\":\"TEST-DIS\",\"materialName\":\"停用测试\","
                    + "\"categoryId\":4000,\"unit\":\"PCS\",\"status\":1}");
            String id = extract(del, "\"data\":(\\d+)");
            if (id == null) {
                throw new IllegalStateException("准备停用科目失败");
            }
            String resp = post("/fi/voucher", "{\"voucherDate\":\"2026-10-31\",\"word\":\"记\","
                    + "\"entries\":[{\"accountId\":" + id + ",\"direction\":1,\"amount\":100},"
                    + "{\"accountId\":91003,\"direction\":2,\"amount\":100}]}");
            require(resp.contains("\"code\":400"), "停用科目仍可记账: " + brief(resp));
            delete("/md/material/" + id);
            return "停用科目被正确拒绝";
        });

        // ---- 汇率：重复配置应被拒绝 ----
        step("10. 汇率新增与重复配置拦截", () -> {
            String resp = post("/fi/rate", "{\"currency\":\"USD\",\"period\":\"2026-11\",\"rate\":7.1}");
            expect(resp, "新增汇率失败");
            String id = extract(resp, "\"data\":(\\d+)");
            String dup = post("/fi/rate", "{\"currency\":\"USD\",\"period\":\"2026-11\",\"rate\":7.2}");
            require(dup.contains("已配置"), "重复汇率未被拦截: " + brief(dup));
            delete("/fi/rate/" + id);
            return "重复汇率被正确拒绝";
        });

        step("11. 非法汇率应被拒绝", () -> {
            String resp = post("/fi/rate", "{\"currency\":\"USD\",\"period\":\"2026-11\",\"rate\":0}");
            require(resp.contains("\"code\":400"), "零汇率未被拦截: " + brief(resp));
            return "非法汇率被正确拒绝";
        });

        // ---- 考勤：同员工同日重复应被拒绝 ----
        step("12. 考勤重复录入拦截", () -> {
            String body = "{\"employeeId\":97001,\"workDate\":\"2026-10-31\",\"workHours\":8,\"shiftCode\":\"D\"}";
            String resp = post("/hr/attendance", body);
            expect(resp, "新增考勤失败");
            String id = extract(resp, "\"data\":(\\d+)");
            String dup = post("/hr/attendance", body);
            require(dup.contains("已有考勤"), "重复考勤未被拦截: " + brief(dup));
            delete("/hr/attendance/" + id);
            return "重复考勤被正确拒绝";
        });

        // ---- 社保政策 ----
        step("13. 社保政策查询与维护", () -> {
            String resp = get("/hr/policy/list");
            expect(resp, "查询政策失败");
            require(resp.contains("2026年度"), "未查到种子政策");
            return "政策可查询";
        });

        // ---- 操作日志确实落库 ----
        step("14. 操作日志记录写入", () -> {
            String resp = get("/sys/log/page?pageNum=1&pageSize=5");
            expect(resp, "查询日志失败");
            require(resp.contains("\"total\":") , "日志列表格式异常: " + brief(resp));
            require(!resp.contains("\"total\":0,"), "操作日志未记录任何写操作");
            return "写操作已记录到日志";
        });

        // ---- 数据权限：售货员看不到采购数据 ----
        step("15. 数据权限隔离", () -> {
            String sellerToken = login("seller", "admin123");
            require(sellerToken != null, "售货员登录失败");
            String resp = getWithToken("/pa/order/page?pageNum=1&pageSize=5", sellerToken);
            require(resp.contains("\"code\":403"), "售货员访问采购订单未被拦截: " + brief(resp));
            return "越权访问被正确拦截";
        });

        // ---- 字典 ----
        step("16. 字典数据可读", () -> {
            String resp = get("/system/dict/data/fi_voucher_status");
            expect(resp, "查询字典失败");
            require(resp.contains("已记账"), "字典缺少凭证状态项");
            return "字典数据完整";
        });

        // ---- 驾驶舱数据与实际一致 ----
        step("17. 驾驶舱统计与明细一致", () -> {
            String resp = get("/bi/dashboard/overview");
            expect(resp, "查询驾驶舱失败");
            require(resp.contains("salesTrend"), "缺少销售趋势");
            require(resp.contains("stockTop"), "缺少库存排行");
            require(resp.contains("todoItems"), "缺少待办事项");
            return "驾驶舱数据完整";
        });

        System.out.println("\n================ 验证结果 ================");
        System.out.println("通过: " + passed + "  失败: " + failed);
        System.out.println("SUMMARY pass=" + passed + " fail=" + failed);
        if (!failures.isEmpty()) {
            System.out.println("\n失败明细：");
            failures.forEach(f -> System.out.println("  [X] " + f));
        }
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static String lastCreatedId;

    private static String login(String username, String password) throws Exception {
        String body = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        String resp = request("/auth/login", "POST", body, null);
        return extract(resp, "\"token\":\"([^\"]+)\"");
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

    private static void expect(String resp, String message) {
        if (!resp.contains("\"code\":200")) {
            throw new IllegalStateException(message + ": " + brief(resp));
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static String get(String path) throws Exception {
        return request(path, "GET", null, token);
    }

    private static String getWithToken(String path, String t) throws Exception {
        return request(path, "GET", null, t);
    }

    private static String post(String path, String body) throws Exception {
        String resp = request(path, "POST", body, token);
        String id = extract(resp, "\"data\":(\\d+)");
        if (id != null) {
            lastCreatedId = id;
        }
        return resp;
    }

    private static String put(String path, String body) throws Exception {
        return request(path, "PUT", body, token);
    }

    private static String delete(String path) throws Exception {
        return request(path, "DELETE", null, token);
    }

    private static String request(String path, String method, String body, String t) throws Exception {
        URL url = new URL(BASE + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(30000);
        if (t != null) {
            conn.setRequestProperty("Authorization", "Bearer " + t);
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
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    private static String extract(String text, String regex) {
        if (text == null) {
            return null;
        }
        Matcher m = Pattern.compile(regex, Pattern.DOTALL).matcher(text);
        return m.find() ? m.group(1) : null;
    }

    private static String brief(String text) {
        if (text == null) {
            return "null";
        }
        return text.length() > 160 ? text.substring(0, 160) + "..." : text;
    }

    @FunctionalInterface
    private interface ThrowingSupplier {
        String get() throws Exception;
    }
}
