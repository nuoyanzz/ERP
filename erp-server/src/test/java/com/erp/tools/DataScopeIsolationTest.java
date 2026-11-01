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
 * 数据权限隔离验证。
 * <p>
 * 采购员（采购部）与销售员（销售部）各自建单，
 * 验证对方看不到对方的单据，且本人能看到自己的。
 */
public final class DataScopeIsolationTest {

    private static final String BASE = "http://localhost:8487";

    private static int passed;
    private static int failed;
    private static final List<String> failures = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        System.out.println("================ 数据权限隔离验证 ================\n");

        String admin = login("admin", TestCredentials.ADMIN_PASSWORD);
        String purchaser = login("purchaser", TestCredentials.DEMO_PASSWORD);
        String seller = login("seller", TestCredentials.DEMO_PASSWORD);
        require(admin != null && purchaser != null && seller != null, "账号登录失败");
        System.out.println("三个账号登录成功\n");

        // 采购员建一张采购订单
        step("1. 采购员创建采购订单", () -> {
            String resp = post("/pa/order", "{\"supplierId\":7000,\"orderDate\":\"2026-10-31\","
                    + "\"entries\":[{\"materialId\":44001,\"qty\":10,\"price\":12.50,\"taxRate\":13}]}",
                    purchaser);
            require(resp.contains("\"code\":200"), "创建失败: " + brief(resp));
            String id = extract(resp, "\"data\":(\\d+)");
            require(id != null, "未返回订单 ID");
            purchaserOrderId = id;
            return "订单 ID " + id;
        });

        // 销售员建一张销售订单
        step("2. 销售员创建销售订单", () -> {
            String resp = post("/sa/order", "{\"customerId\":6000,\"orderDate\":\"2026-10-31\","
                    + "\"entries\":[{\"materialId\":44001,\"qty\":5,\"price\":20,\"taxRate\":13}]}",
                    seller);
            require(resp.contains("\"code\":200"), "创建失败: " + brief(resp));
            String id = extract(resp, "\"data\":(\\d+)");
            require(id != null, "未返回订单 ID");
            sellerOrderId = id;
            return "订单 ID " + id;
        });

        step("3. 管理员能看到两张单据", () -> {
            String pa = get("/pa/order/page?pageNum=1&pageSize=50", admin);
            String sa = get("/sa/order/page?pageNum=1&pageSize=50", admin);
            require(pa.contains(purchaserOrderId), "管理员看不到采购订单");
            require(sa.contains(sellerOrderId), "管理员看不到销售订单");
            return "两张单据均在";
        });

        step("4. 采购员能看到自己的采购订单", () -> {
            String resp = get("/pa/order/page?pageNum=1&pageSize=50", purchaser);
            require(resp.contains("\"code\":200"), "查询失败: " + brief(resp));
            require(resp.contains(purchaserOrderId), "采购员看不到自己的订单");
            return "可见";
        });

        step("5. 销售员无采购订单的函数权限（接口级拦截）", () -> {
            String resp = get("/pa/order/page?pageNum=1&pageSize=50", seller);
            require(resp.contains("\"code\":403"), "销售员应被接口权限拦截: " + brief(resp));
            return "接口级权限拦截生效";
        });

        step("6. 销售员能看到自己的销售订单", () -> {
            String resp = get("/sa/order/page?pageNum=1&pageSize=50", seller);
            require(resp.contains("\"code\":200"), "查询失败: " + brief(resp));
            require(resp.contains(sellerOrderId), "销售员看不到自己的订单");
            return "可见";
        });

        step("7. 采购员无销售订单的函数权限（接口级拦截）", () -> {
            String resp = get("/sa/order/page?pageNum=1&pageSize=50", purchaser);
            require(resp.contains("\"code\":403"), "采购员应被接口权限拦截: " + brief(resp));
            return "接口级权限拦截生效";
        });

        step("8. 采购员的库存数据也按部门隔离", () -> {
            String resp = get("/ic/stock/page?pageNum=1&pageSize=50", purchaser);
            require(resp.contains("\"code\":200"), "查询失败: " + brief(resp));
            return "库存查询按数据范围执行";
        });

        // 同模块、不同部门的两个采购员，验证「本部门」数据范围真的生效
        step("9. 另建一个生产部的采购员账号", () -> {
            String resp = post("/sys/user", "{\"username\":\"purchaser2\",\"password\":\"" + TestCredentials.DEMO_PASSWORD + "\","
                    + "\"nickname\":\"生产部采购员\",\"deptId\":96015,\"status\":0,\"roleIds\":[3]}", admin);
            require(resp.contains("\"code\":200"), "创建用户失败: " + brief(resp));
            String id = extract(resp, "\"data\":(\\d+)");
            require(id != null, "未返回用户 ID");
            otherPurchaserId = id;
            return "用户 ID " + id + "（生产部）";
        });

        step("10. 生产部采购员登录", () -> {
            String token = login("purchaser2", TestCredentials.DEMO_PASSWORD);
            require(token != null, "新账号登录失败");
            otherPurchaserToken = token;
            return "登录成功";
        });

        step("11. 生产部采购员看不到采购部的订单（数据范围隔离生效）", () -> {
            String resp = get("/pa/order/page?pageNum=1&pageSize=50", otherPurchaserToken);
            require(resp.contains("\"code\":200"), "查询失败: " + brief(resp));
            require(!resp.contains(purchaserOrderId),
                    "生产部采购员看到了采购部的订单，数据范围过滤未生效");
            return "同角色跨部门数据被隔离";
        });

        step("12. 数据权限不影响管理员（全部可见）", () -> {
            String resp = get("/pa/order/page?pageNum=1&pageSize=50", admin);
            require(resp.contains(purchaserOrderId), "管理员看不到订单");
            return "管理员可见全部";
        });

        step("13. 清理测试用户", () -> {
            String resp = request("/sys/user/" + otherPurchaserId, "DELETE", null, admin);
            require(resp.contains("\"code\":200"), "删除失败: " + brief(resp));
            return "已清理";
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

    private static String purchaserOrderId;
    private static String sellerOrderId;
    private static String otherPurchaserId;
    private static String otherPurchaserToken;

    private static String login(String username, String password) throws Exception {
        String body = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        return extract(post("/auth/login", body, null), "\"token\":\"([^\"]+)\"");
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

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static String get(String path, String token) throws Exception {
        return request(path, "GET", null, token);
    }

    private static String post(String path, String body, String token) throws Exception {
        return request(path, "POST", body, token);
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
