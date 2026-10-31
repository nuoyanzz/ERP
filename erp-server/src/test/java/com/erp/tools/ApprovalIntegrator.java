package com.erp.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 审批流接入工具。
 * <p>
 * 在业务审核方法开头插入审批流判断，方法体保持不变——审批通过后由回调再次调用该方法，
 * 真正执行审核动作。
 */
public final class ApprovalIntegrator {

    /**
     * 接入点描述。
     */
    private record Target(
            String file,
            String method,
            /** 审核方法内取单据实体的变量名 */
            String entityVar,
            /** 单据号字段 */
            String noField,
            /** 单据金额字段，可为 null */
            String amountField,
            /** 业务类型 */
            String bizType,
            /** listener 类名 */
            String listenerClass,
            /** 审批标题前缀 */
            String titlePrefix
    ) {
    }

    private static final List<Target> TARGETS = List.of(
            new Target("sa/service/impl/SaOrderServiceImpl.java", "auditOrder", "exist", "orderNo",
                    "totalAmount", "SA_ORDER", "SAOrderApprovalListener", "销售订单"),
            new Target("ic/service/impl/IcCheckServiceImpl.java", "auditCheck", "check", "checkNo",
                    null, "IC_CHECK", "IcCheckApprovalListener", "库存盘点"),
            new Target("ic/service/impl/IcTransferServiceImpl.java", "auditTransfer", "transfer", "transferNo",
                    null, "IC_TRANSFER", "IcTransferApprovalListener", "库存调拨"),
            new Target("pm/service/impl/PmMoServiceImpl.java", "auditMo", "mo", "moNo",
                    "qty", "PM_MO", "PMMoApprovalListener", "生产订单"),
            new Target("pm/service/impl/PmPickServiceImpl.java", "auditPick", "pick", "pickNo",
                    null, "PM_PICK", "PMPickApprovalListener", "生产领料单")
    );

    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]);
        for (Target target : TARGETS) {
            Path file = root.resolve(target.file());
            if (!Files.exists(file)) {
                System.out.println("[SKIP] 文件不存在: " + target.file());
                continue;
            }
            String source = Files.readString(file, StandardCharsets.UTF_8);
            if (source.contains("approvalGate.tryStart")) {
                System.out.println("[SKIP] 已接入: " + target.file());
                continue;
            }
            String updated = inject(source, target);
            if (updated == null) {
                System.out.println("[FAIL] 未定位到方法: " + target.method() + " in " + target.file());
                continue;
            }
            updated = addImports(updated, target);
            updated = addDependency(updated, target);
            Files.writeString(file, updated, StandardCharsets.UTF_8);
            System.out.println("[OK] " + target.file());
        }
    }

    /**
     * 在审核方法的状态校验之后插入审批流判断。
     */
    private static String inject(String source, Target target) {
        Pattern pattern = Pattern.compile(
                "(\\s*public void " + Pattern.quote(target.method()) + "\\(Long id\\) \\{.*?"
                        + "throw new BizException\\(\"[^\"]*审核\"\\);\\s*\\n)",
                Pattern.DOTALL);
        Matcher matcher = pattern.matcher(source);
        if (!matcher.find()) {
            return null;
        }
        String amountExpr = target.amountField() == null
                ? "null"
                : target.entityVar() + ".get" + capitalize(target.amountField()) + "()";
        String titleExpr = "\"" + target.titlePrefix() + "\" + " + target.entityVar() + ".get"
                + capitalize(target.noField()) + "()";
        String insert = "\n        // 若该业务类型配置了审批流，审核动作改由审批通过后回调触发\n"
                + "        if (approvalGate.tryStart(" + target.listenerClass() + ".BIZ_TYPE, id,\n"
                + "                " + titleExpr + ", " + amountExpr + ", null)) {\n"
                + "            return;\n"
                + "        }\n";
        return source.substring(0, matcher.end()) + insert + source.substring(matcher.end());
    }

    private static String addImports(String source, Target target) {
        String imports = "import com.erp.wf.integration.ApprovalGate;\n"
                + "import com.erp.wf.integration." + target.listenerClass() + ";";
        if (source.contains("import com.erp.wf.integration.ApprovalGate;")) {
            return source;
        }
        // 插到第一个 com.erp import 之前
        Matcher m = Pattern.compile("^import com\\.erp\\.[^\\n]*;\\n", Pattern.MULTILINE).matcher(source);
        if (m.find()) {
            return source.substring(0, m.start()) + imports + "\n" + source.substring(m.start());
        }
        return source;
    }

    private static String addDependency(String source, Target target) {
        // 在最后一个 private final 之后插入
        Matcher m = Pattern.compile("(\\n\\s*private final [^\\n]+;\\n)").matcher(source);
        String last = null;
        int end = 0;
        while (m.find()) {
            last = m.group(1);
            end = m.end();
        }
        if (last == null) {
            return source;
        }
        String dep = "\n    private final ApprovalGate approvalGate;\n";
        return source.substring(0, end) + dep + source.substring(end);
    }

    private static String capitalize(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}








