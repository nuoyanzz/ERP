package com.erp.tools;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 全量接口巡检。
 * <p>
 * 对每个模块的关键接口发起真实请求，检查：
 *   - HTTP 状态码是否为 5xx（服务端异常说明有缺陷）
 *   - 业务响应码是否为 200
 *   - 无权限用户能否越权访问
 */
public final class ApiSmokeTest {

    private static final String BASE = "http://localhost:8487";

    private static String adminToken;
    private static String sellerToken;

    private static int passed;
    private static int failed;
    private static final List<String> failures = new ArrayList<>();

    /** 接口定义：模块、方法、路径、所需权限点 */
    private record Endpoint(String module, String method, String path, String perm, String desc) {
    }

    public static void main(String[] args) throws Exception {
        System.out.println("================ 全量接口巡检 ================\n");

        // 登录两个不同权限的账号，用于越权检测
        adminToken = login("admin", TestCredentials.ADMIN_PASSWORD);
        sellerToken = login("seller", TestCredentials.DEMO_PASSWORD);
        if (adminToken == null) {
            System.out.println("管理员登录失败，终止");
            System.exit(1);
        }
        System.out.println("管理员登录成功，售货员登录：" + (sellerToken != null ? "成功" : "失败") + "\n");

        List<Endpoint> endpoints = buildEndpoints();

        System.out.println("--- 1. 管理员访问全部接口（正常路径）---");
        for (Endpoint e : endpoints) {
            check(e, adminToken, true);
        }

        System.out.println("\n--- 2. 低权限账号越权检测 ---");
        for (Endpoint e : endpoints) {
            // 财务与系统管理属于敏感区域，售货员不应访问
            if (e.perm() != null && (e.perm().startsWith("fi:") || e.perm().startsWith("sys:")
                    || e.perm().startsWith("hr:") || e.perm().startsWith("pm:"))) {
                check(e, sellerToken, false);
            }
        }

        System.out.println("\n--- 3. 未认证访问（需权限的接口应返回 401）---");
        for (Endpoint e : endpoints) {
            // 登录与字典这类公开接口不校验权限
            if (e.perm() != null) {
                checkStatus(e, null, 401, "未认证应返回 401");
            }
        }

        System.out.println("\n================ 巡检结果 ================");
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

    private static String login(String username, String password) throws Exception {
        String body = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        String resp = request("/auth/login", "POST", body, null);
        return extract(resp, "\"token\":\"([^\"]+)\"");
    }

    /**
     * 检查接口：正常账号应成功，低权限账号应被拒绝。
     */
    private static void check(Endpoint e, String token, boolean expectSuccess) throws Exception {
        String resp = request(e.path(), e.method(), null, token);
        int httpStatus = lastStatus;
        String code = extract(resp, "\"code\":(\\d+)");

        if (expectSuccess) {
            boolean serverError = httpStatus >= 500;
            boolean bizError = code != null && !"200".equals(code);
            if (serverError || bizError) {
                failed++;
                failures.add(String.format("%s %s (%s) — %s%s", e.method(), e.path(), e.desc(),
                        httpStatus >= 500 ? "HTTP " + httpStatus : "业务码 " + code,
                        bizError && !serverError ? " " + extract(resp, "\"message\":\"([^\"]*)\"") : ""));
            } else {
                passed++;
            }
        } else {
            // 低权限账号访问敏感接口：返回 403 业务码即正确
            boolean forbidden = "403".equals(code) || httpStatus == 403;
            if (forbidden) {
                passed++;
            } else {
                failed++;
                failures.add(String.format("越权未拦截：%s %s (%s) — 返回 %s / %s",
                        e.method(), e.path(), e.desc(), httpStatus, resp.length() > 80
                        ? resp.substring(0, 80) : resp));
            }
        }
    }

    private static void checkStatus(Endpoint e, String token, int expect, String desc) throws Exception {
        String resp = request(e.path(), e.method(), null, token);
        if (lastStatus == expect) {
            passed++;
        } else {
            failed++;
            failures.add(String.format("%s %s — %s，期望 HTTP %d 实际 %d", e.method(), e.path(),
                    desc, expect, lastStatus));
        }
    }

    private static int lastStatus;

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
        lastStatus = code;
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
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile(regex, java.util.regex.Pattern.DOTALL).matcher(text);
        return m.find() ? m.group(1) : null;
    }

    /**
     * 待巡检的接口清单。
     */
    private static List<Endpoint> buildEndpoints() {
        List<Endpoint> list = new ArrayList<>();
        // 只读接口适合批量巡检；写接口单独在业务 E2E 里覆盖
        // 登录接口需要请求体，不纳入无参巡检


        list.add(new Endpoint("主数据", "GET", "/md/material/page?pageNum=1&pageSize=5", "md:material:query", "物料列表"));
        list.add(new Endpoint("主数据", "GET", "/md/material-category/tree", "md:material-category:query", "物料分类树"));
        list.add(new Endpoint("主数据", "GET", "/md/customer/page?pageNum=1&pageSize=5", "md:customer:query", "客户列表"));
        list.add(new Endpoint("主数据", "GET", "/md/supplier/page?pageNum=1&pageSize=5", "md:supplier:query", "供应商列表"));
        list.add(new Endpoint("主数据", "GET", "/md/warehouse/page?pageNum=1&pageSize=5", "md:warehouse:query", "仓库列表"));

        list.add(new Endpoint("采购", "GET", "/pa/order/page?pageNum=1&pageSize=5", "pa:order:query", "采购订单"));
        list.add(new Endpoint("采购", "GET", "/pa/instock/page?pageNum=1&pageSize=5", "pa:instock:query", "采购入库"));
        list.add(new Endpoint("采购", "GET", "/pa/return/page?pageNum=1&pageSize=5", "pa:return:query", "采购退货"));
        list.add(new Endpoint("销售", "GET", "/sa/order/page?pageNum=1&pageSize=5", "sa:order:query", "销售订单"));
        list.add(new Endpoint("销售", "GET", "/sa/outstock/page?pageNum=1&pageSize=5", "sa:outstock:query", "销售出库"));
        list.add(new Endpoint("销售", "GET", "/sa/return/page?pageNum=1&pageSize=5", "sa:return:query", "销售退货"));
        list.add(new Endpoint("库存", "GET", "/ic/stock/page?pageNum=1&pageSize=5", "ic:stock:query", "即时库存"));
        list.add(new Endpoint("库存", "GET", "/ic/transaction/page?pageNum=1&pageSize=5", "ic:transaction:query", "库存流水"));
        list.add(new Endpoint("库存", "GET", "/ic/transfer/page?pageNum=1&pageSize=5", "ic:transfer:query", "库存调拨"));
        list.add(new Endpoint("库存", "GET", "/ic/check/page?pageNum=1&pageSize=5", "ic:check:query", "库存盘点"));

        list.add(new Endpoint("财务", "GET", "/fi/account/tree", "fi:account:query", "科目树"));
        list.add(new Endpoint("财务", "GET", "/fi/period/list?periodYear=2026", "fi:period:query", "会计期间"));
        list.add(new Endpoint("财务", "GET", "/fi/voucher/page?pageNum=1&pageSize=5", "fi:voucher:query", "记账凭证"));
        list.add(new Endpoint("财务", "GET", "/fi/balance/account?period=2026-10", "fi:balance:query", "科目余额表"));
        list.add(new Endpoint("财务", "GET", "/fi/report/balance-sheet?period=2026-10", "fi:report:query", "资产负债表"));
        list.add(new Endpoint("财务", "GET", "/fi/report/income-statement?period=2026-10", "fi:report:query", "利润表"));
        list.add(new Endpoint("财务", "GET", "/fi/report/cash-flow-statement?period=2026-10", "fi:report:query", "现金流量表"));
        list.add(new Endpoint("财务", "GET", "/fi/opening/list?period=2026-10", "fi:opening:query", "期初余额"));
        list.add(new Endpoint("财务", "GET", "/fi/opening/trial-balance?period=2026-10", "fi:trial:query", "试算平衡"));
        list.add(new Endpoint("财务", "GET", "/fi/rate/page?pageNum=1&pageSize=5", "fi:rate:query", "汇率维护"));

        list.add(new Endpoint("应收", "GET", "/ar/receivable/page?pageNum=1&pageSize=5", "ar:receivable:query", "应收单"));
        list.add(new Endpoint("应收", "GET", "/ar/receipt/page?pageNum=1&pageSize=5", "ar:receipt:query", "收款单"));
        list.add(new Endpoint("应收", "GET", "/ar/receivable/aging", "ar:aging:query", "账龄分析"));
        list.add(new Endpoint("应付", "GET", "/ap/payable/page?pageNum=1&pageSize=5", "ap:payable:query", "应付单"));
        list.add(new Endpoint("应付", "GET", "/ap/payment/page?pageNum=1&pageSize=5", "ap:payment:query", "付款单"));

        list.add(new Endpoint("生产", "GET", "/pm/bom/page?pageNum=1&pageSize=5", "pm:bom:query", "BOM"));
        list.add(new Endpoint("生产", "GET", "/pm/mo/page?pageNum=1&pageSize=5", "pm:mo:query", "生产订单"));
        list.add(new Endpoint("生产", "GET", "/pm/pick/page?pageNum=1&pageSize=5", "pm:pick:query", "生产领料"));
        list.add(new Endpoint("生产", "GET", "/pm/finish/page?pageNum=1&pageSize=5", "pm:finish:query", "完工入库"));
        list.add(new Endpoint("生产", "GET", "/pm/mrp/page?pageNum=1&pageSize=5", "pm:mrp:query", "MRP 结果"));
        list.add(new Endpoint("生产", "GET", "/pm/mrp/latest-run-no", "pm:mrp:query", "MRP 最新批次"));
        list.add(new Endpoint("生产", "GET", "/pm/routing/page?pageNum=1&pageSize=5", "pm:routing:query", "工艺路线"));
        list.add(new Endpoint("生产", "GET", "/pm/routing/work-centers", "pm:routing:query", "工作中心"));
        list.add(new Endpoint("生产", "GET", "/pm/report/page?pageNum=1&pageSize=5", "pm:report:query", "生产报工"));
        list.add(new Endpoint("生产", "GET", "/pm/cost/page?pageNum=1&pageSize=5", "pm:cost:query", "成本归集"));
        list.add(new Endpoint("生产", "GET", "/pm/cost/list", "pm:cost:query", "成本列表"));

        list.add(new Endpoint("工作流", "GET", "/wf/definition/page?pageNum=1&pageSize=5", "wf:definition:query", "流程定义"));
        list.add(new Endpoint("工作流", "GET", "/wf/definition/biz-type/PA_ORDER", "wf:definition:query", "按业务类型查流程"));
        list.add(new Endpoint("工作流", "GET", "/wf/definition/biz-type/PA_ORDER/published", "wf:definition:query", "已发布流程"));
        list.add(new Endpoint("工作流", "GET", "/wf/instance/page?pageNum=1&pageSize=5", "wf:instance:query", "流程实例"));
        list.add(new Endpoint("工作流", "GET", "/wf/task/todo?pageNum=1&pageSize=5", "wf:task:todo", "我的待办"));
        list.add(new Endpoint("工作流", "GET", "/wf/task/done?pageNum=1&pageSize=5", "wf:task:done", "我的已办"));

        list.add(new Endpoint("人力", "GET", "/hr/org/page?pageNum=1&pageSize=5", "hr:org:query", "组织"));
        list.add(new Endpoint("人力", "GET", "/hr/org/list", "hr:org:query", "组织列表"));
        list.add(new Endpoint("人力", "GET", "/hr/dept/tree", "hr:dept:query", "部门树"));
        list.add(new Endpoint("人力", "GET", "/hr/employee/page?pageNum=1&pageSize=5", "hr:employee:query", "员工档案"));
        list.add(new Endpoint("人力", "GET", "/hr/salary/page?pageNum=1&pageSize=5", "hr:salary:query", "薪资单"));
        list.add(new Endpoint("人力", "GET", "/hr/attendance/page?pageNum=1&pageSize=5", "hr:attendance:query", "考勤"));
        list.add(new Endpoint("人力", "GET", "/hr/policy/list", "hr:policy:query", "社保个税政策"));

        list.add(new Endpoint("系统", "GET", "/sys/user/page?pageNum=1&pageSize=5", "sys:user:query", "用户列表"));
        list.add(new Endpoint("系统", "GET", "/sys/role/page?pageNum=1&pageSize=5", "sys:role:query", "角色列表"));
        list.add(new Endpoint("系统", "GET", "/sys/role/list", "sys:role:query", "角色列表项"));
        list.add(new Endpoint("系统", "GET", "/sys/menu/tree", "sys:menu:query", "菜单树"));
        list.add(new Endpoint("系统", "GET", "/sys/menu/my", null, "我的菜单"));
        list.add(new Endpoint("系统", "GET", "/sys/menu/my-perms", null, "我的权限码"));
        list.add(new Endpoint("系统", "GET", "/sys/log/page?pageNum=1&pageSize=5", "sys:log:query", "操作日志"));
        list.add(new Endpoint("系统", "GET", "/system/dict/types", null, "字典类型"));
        list.add(new Endpoint("系统", "GET", "/system/dict/data/pa_order_status", null, "字典项"));

        list.add(new Endpoint("驾驶舱", "GET", "/bi/dashboard/overview", null, "工作台统计"));

        list.add(new Endpoint("主数据", "GET", "/md/unit/page?pageNum=1&pageSize=5", "md:unit:query", "计量单位"));
        list.add(new Endpoint("主数据", "GET", "/md/price/page?pageNum=1&pageSize=5", "md:price:query", "价格表"));
        list.add(new Endpoint("主数据", "GET", "/md/unit-convert/page?pageNum=1&pageSize=5", "md:unitConvert:query", "单位换算"));
        list.add(new Endpoint("财务", "GET", "/fi/rate/page?pageNum=1&pageSize=5", "fi:rate:query", "汇率维护"));
        list.add(new Endpoint("财务", "GET", "/fi/rule/page?pageNum=1&pageSize=5", "fi:rule:query", "科目映射规则"));
        list.add(new Endpoint("财务", "GET", "/fi/trial/balance?period=2026-10", "fi:trial:query", "试算平衡"));
        list.add(new Endpoint("应收", "GET", "/ap/aging", "ap:aging:query", "应付账龄"));
        list.add(new Endpoint("应收", "GET", "/ar/baddebt/page?pageNum=1&pageSize=5", "ar:baddebt:query", "坏账计提"));
        list.add(new Endpoint("应收", "GET", "/ar/recon?customerId=6000", "ar:recon:query", "往来对账"));
        list.add(new Endpoint("人力", "GET", "/hr/attendance/page?pageNum=1&pageSize=5", "hr:attendance:query", "考勤管理"));
        list.add(new Endpoint("人力", "GET", "/hr/policy/list", "hr:policy:query", "社保个税政策"));
        list.add(new Endpoint("系统", "GET", "/sys/param/page?pageNum=1&pageSize=5", "sys:param:query", "系统参数"));
        list.add(new Endpoint("系统", "GET", "/sys/log/page?pageNum=1&pageSize=5", "sys:log:query", "操作日志"));
        list.add(new Endpoint("系统", "GET", "/system/dict/types", null, "字典类型"));

        return list;
    }
}
