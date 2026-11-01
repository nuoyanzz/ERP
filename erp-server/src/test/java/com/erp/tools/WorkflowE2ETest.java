package com.erp.tools;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 工作流端到端验证。
 * <p>
 * 覆盖：发起审批 → 条件分支 → 多级审批 → 业务单据状态变更 → 驳回路径。
 */
public final class WorkflowE2ETest {

    private static final String BASE = "http://localhost:8487";

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("============ 工作流端到端验证 ============\n");

        final String[] tokens = new String[3];

        step("1. 采购员登录", () -> {
            String resp = post("/auth/login", "{\"username\":\"purchaser\",\"password\":\"" + TestCredentials.DEMO_PASSWORD + "\"}");
            tokens[0] = extract(resp, "\"token\":\"([^\"]+)\"");
            require(tokens[0] != null, "采购员登录失败: " + brief(resp));
            return "token 长度 " + tokens[0].length();
        });

        step("2. 财务主管登录", () -> {
            String resp = post("/auth/login", "{\"username\":\"finance\",\"password\":\"" + TestCredentials.DEMO_PASSWORD + "\"}");
            tokens[1] = extract(resp, "\"token\":\"([^\"]+)\"");
            require(tokens[1] != null, "财务主管登录失败: " + brief(resp));
            return "token 长度 " + tokens[1].length();
        });

        step("2b. 管理员登录（用于跨模块核对单据状态）", () -> {
            String resp = post("/auth/login", "{\"username\":\"admin\",\"password\":\"" + TestCredentials.ADMIN_PASSWORD + "\"}");
            tokens[2] = extract(resp, "\"token\":\"([^\"]+)\"");
            require(tokens[2] != null, "管理员登录失败: " + brief(resp));
            return "token 长度 " + tokens[2].length();
        });

        final String pToken = tokens[0];
        final String fToken = tokens[1];
        final String adminToken = tokens[2];

        // 小额订单：应只走采购主管审批
        final String[] smallOrder = new String[1];
        step("3. 采购员创建小额订单（< 5 万）", () -> {
            String resp = post("/pa/order", "{\"supplierId\":7000,\"orderDate\":\"2026-10-31\","
                    + "\"entries\":[{\"materialId\":44001,\"qty\":10,\"price\":12.50,\"taxRate\":13}]}", pToken);
            check(resp.contains("\"code\":200"), "创建失败: " + brief(resp));
            smallOrder[0] = extract(resp, "\"data\":(\\d+)");
            require(smallOrder[0] != null, "未返回订单 ID");
            return "订单 ID " + smallOrder[0] + "（金额 141.25）";
        });

        step("4. 提交审核，发起审批流", () -> {
            String resp = post("/pa/order/" + smallOrder[0] + "/audit", null, pToken);
            check(resp.contains("\"code\":200"), "提交失败: " + brief(resp));
            return "已发起审批";
        });

        step("5. 订单在审批中应保持暂存", () -> {
            String resp = get("/pa/order/" + smallOrder[0], pToken);
            check(resp.contains("\"status\":0"), "审批中订单应保持暂存状态: " + brief(resp));
            return "订单状态 = 暂存";
        });

        step("6. 采购员收到待办", () -> {
            String resp = get("/wf/task/todo?pageNum=1&pageSize=5&bizType=PA_ORDER&bizId=" + smallOrder[0], pToken);
            check(resp.contains("\"code\":200"), "查询待办失败: " + brief(resp));
            check(!resp.contains("\"records\":[]"), "采购员应收到待办任务");
            return "待办已生成";
        });

        final String[] taskId = new String[1];
        step("7. 取出待办任务 ID", () -> {
            String resp = get("/wf/task/todo?pageNum=1&pageSize=5&bizType=PA_ORDER&bizId=" + smallOrder[0], pToken);
            Matcher m = Pattern.compile("\"id\":(\\d+)[^}]*\"nodeName\":\"([^\"]*)\"").matcher(resp);
            require(m.find(), "未找到待办任务: " + brief(resp));
            taskId[0] = m.group(1);
            return "任务 " + taskId[0] + " 节点「" + m.group(2) + "」";
        });

        step("8. 采购员审批通过", () -> {
            String resp = post("/wf/task/" + taskId[0] + "/approve", "{\"comment\":\"同意采购\"}", pToken);
            check(resp.contains("\"code\":200"), "审批失败: " + brief(resp));
            return "审批通过";
        });

        step("9. 审批通过后订单变为已审核", () -> {
            // 业务回调在事务提交后执行，这里轮询等待
            String resp = waitFor(() -> safeGet("/pa/order/" + smallOrder[0], pToken),
                    r -> r.contains("\"status\":1"), 8000);
            check(resp.contains("\"status\":1"), "订单应变为已审核: " + brief(resp));
            return "订单状态 = 已审核（业务动作由审批回调触发）";
        });

        // 大额订单：应触发财务总监审批分支
        final String[] bigOrder = new String[1];
        step("10. 采购员创建大额订单（> 5 万）", () -> {
            String resp = post("/pa/order", "{\"supplierId\":7000,\"orderDate\":\"2026-10-31\","
                    + "\"entries\":[{\"materialId\":44001,\"qty\":5000,\"price\":12.50,\"taxRate\":13}]}", pToken);
            check(resp.contains("\"code\":200"), "创建失败: " + brief(resp));
            bigOrder[0] = extract(resp, "\"data\":(\\d+)");
            require(bigOrder[0] != null, "未返回订单 ID");
            return "订单 ID " + bigOrder[0] + "（金额 70625）";
        });

        step("11. 大额订单提交审批", () -> {
            String resp = post("/pa/order/" + bigOrder[0] + "/audit", null, pToken);
            check(resp.contains("\"code\":200"), "提交失败: " + brief(resp));
            return "已发起审批";
        });

        step("12. 采购员通过第一级后转财务总监", () -> {
            String resp = get("/wf/task/todo?pageNum=1&pageSize=5&bizType=PA_ORDER&bizId=" + bigOrder[0], pToken);
            Matcher m = Pattern.compile("\"id\":(\\d+)[^}]*\"nodeName\":\"([^\"]*)\"").matcher(resp);
            require(m.find(), "未找到待办: " + brief(resp));
            String id = m.group(1);
            String approve = post("/wf/task/" + id + "/approve", "{\"comment\":\"同意\"}", pToken);
            check(approve.contains("\"code\":200"), "审批失败: " + brief(approve));
            return "第一级审批完成";
        });

        step("13. 财务主管收到财务总监审批待办", () -> {
            // 上一节点通过后才激活下一节点，轮询等待
            String resp = waitFor(() -> safeGet("/wf/task/todo?pageNum=1&pageSize=5&bizType=PA_ORDER&bizId=" + bigOrder[0], fToken),
                    r -> !r.contains("\"records\":[]"), 8000);
            check(resp.contains("\"code\":200"), "查询待办失败: " + brief(resp));
            check(!resp.contains("\"records\":[]"), "财务主管应收到待办");
            Matcher m = Pattern.compile("\"nodeName\":\"([^\"]*)\"").matcher(resp);
            String node = m.find() ? m.group(1) : "";
            check(node.contains("财务总监"), "条件分支应跳转到财务总监节点，实际节点: " + node);
            return "待办节点 = " + node + "（条件分支生效）";
        });

        step("14. 财务总监审批通过后订单已审核", () -> {
            String resp = get("/wf/task/todo?pageNum=1&pageSize=5&bizType=PA_ORDER&bizId=" + bigOrder[0], fToken);
            Matcher m = Pattern.compile("\"id\":(\\d+)").matcher(resp);
            require(m.find(), "未找到财务待办");
            String approve = post("/wf/task/" + m.group(1) + "/approve", "{\"comment\":\"同意\"}", fToken);
            check(approve.contains("\"code\":200"), "审批失败: " + brief(approve));
            // 财务主管无采购订单查询权限，用管理员确认单据状态
            String order = get("/pa/order/" + bigOrder[0], adminToken);
            check(order.contains("\"status\":1"), "订单应变为已审核: " + brief(order));
            return "大额订单两级审批全通过";
        });

        // 驳回路径
        final String[] rejectOrder = new String[1];
        step("15. 新建订单用于验证驳回", () -> {
            String resp = post("/pa/order", "{\"supplierId\":7000,\"orderDate\":\"2026-10-31\","
                    + "\"entries\":[{\"materialId\":44001,\"qty\":5,\"price\":12.50,\"taxRate\":13}]}", pToken);
            rejectOrder[0] = extract(resp, "\"data\":(\\d+)");
            require(rejectOrder[0] != null, "创建失败: " + brief(resp));
            post("/pa/order/" + rejectOrder[0] + "/audit", null, pToken);
            return "订单 ID " + rejectOrder[0];
        });

        step("16. 驳回后订单保持暂存", () -> {
            String resp = get("/wf/task/todo?pageNum=1&pageSize=5&bizType=PA_ORDER&bizId=" + rejectOrder[0], pToken);
            Matcher m = Pattern.compile("\"id\":(\\d+)").matcher(resp);
            require(m.find(), "未找到待办");
            String reject = post("/wf/task/" + m.group(1) + "/reject", "{\"comment\":\"预算不足\"}", pToken);
            check(reject.contains("\"code\":200"), "驳回失败: " + brief(reject));
            String order = get("/pa/order/" + rejectOrder[0], pToken);
            check(order.contains("\"status\":0"), "驳回后订单应保持暂存: " + brief(order));
            return "驳回生效，订单未通过审核";
        });

        step("17. 流程实例记录可追溯", () -> {
            String resp = get("/wf/instance/page?pageNum=1&pageSize=10", adminToken);
            check(resp.contains("\"code\":200"), "查询实例失败: " + brief(resp));
            check(!resp.contains("\"records\":[]"), "应有流程实例记录");
            return "流程实例可追溯";
        });

        step("18. 已办列表记录审批轨迹", () -> {
            String resp = get("/wf/task/done?pageNum=1&pageSize=10", pToken);
            check(resp.contains("\"code\":200"), "查询已办失败: " + brief(resp));
            return "已办列表可查询";
        });

        System.out.println("\n============ 验证结果 ============");
        System.out.println("通过: " + passed + "  失败: " + failed);
        System.out.println("SUMMARY pass=" + passed + " fail=" + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    // ------------------------------------------------------------------
    // HTTP 辅助
    // ------------------------------------------------------------------

    private static String get(String path, String token) throws Exception {
        return request(path, "GET", null, token);
    }

    private static String post(String path, String body, String token) throws Exception {
        return request(path, "POST", body, token);
    }

    private static String post(String path, String body) throws Exception {
        return request(path, "POST", body, null);
    }

    private static String request(String path, String method, String body, String token) throws Exception {
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
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    /**
     * 轮询等待条件成立。业务回调在事务提交后异步执行，需要等待其完成。
     */
    /**
     * 不抛受检异常的 GET，供轮询使用。
     */
    private static String safeGet(String path, String token) {
        try {
            return get(path, token);
        } catch (Exception e) {
            return "";
        }
    }

    private static String waitFor(java.util.function.Supplier<String> supplier,
                                 java.util.function.Predicate<String> condition, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        String last = "";
        while (System.currentTimeMillis() < deadline) {
            try {
                last = supplier.get();
            } catch (Exception e) {
                last = "";
            }
            if (condition.test(last)) {
                return last;
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return last;
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
        return text.length() > 200 ? text.substring(0, 200) + "..." : text;
    }

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

    @FunctionalInterface
    private interface ThrowingSupplier {
        String get() throws Exception;
    }
}








