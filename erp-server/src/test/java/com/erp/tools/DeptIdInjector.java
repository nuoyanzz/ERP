package com.erp.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 批量为单据实体补齐 deptId 字段。
 * <p>
 * 数据权限按部门过滤时需要实体映射到表的 dept_id 列；缺字段的实体
 * 只能退化���「仅本人」模式，部门级隔离失效。
 */
public final class DeptIdInjector {

    /** 需要补齐 deptId 的实体 */
    private static final List<String> TARGETS = List.of(
            "PaInstock", "PaReturn", "SaOutstock", "SaReturn",
            "IcTransfer", "IcCheck", "PmPick", "PmFinish",
            "ArReceivable", "ArReceipt", "ApPayable", "ApPayment");

    public static void main(String[] args) throws IOException {
        Path root = Path.of(args[0]);
        int patched = 0;
        for (String target : TARGETS) {
            Path file = findEntity(root, target);
            if (file == null) {
                System.out.println("[SKIP] 未找到实体: " + target);
                continue;
            }
            String content = Files.readString(file, StandardCharsets.UTF_8);
            if (content.contains("private Long deptId")) {
                System.out.println("[SKIP] 已有 deptId: " + target);
                continue;
            }
            // 在最后一个私有字段之后插入 deptId
            Pattern pattern = Pattern.compile("(\\n\\s+private [\\w<>,\\. ]+ \\w+;\\n)(\\})");
            Matcher matcher = pattern.matcher(content);
            if (!matcher.find()) {
                System.out.println("[FAIL] 未定位到字段区: " + target);
                continue;
            }
            String updated = content.substring(0, matcher.end(1))
                    + "\n    /** 所属部门，数据权限按此字段过滤 */\n    private Long deptId;\n"
                    + content.substring(matcher.end(1));
            Files.writeString(file, updated, StandardCharsets.UTF_8);
            patched++;
            System.out.println("[OK] " + target + " -> " + file.getFileName());
        }
        System.out.println("共补齐 " + patched + " 个实体");
    }

    private static Path findEntity(Path root, String className) throws IOException {
        try (var stream = Files.walk(root)) {
            return stream
                    .filter(p -> p.getFileName().toString().equals(className + ".java"))
                    .findFirst()
                    .orElse(null);
        }
    }
}
