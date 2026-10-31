package com.erp.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 为 Controller 方法批量补充 {@code @RequiresPermission} 与 {@code @OperLog} 注解。
 * <p>
 * 权限码推导规则：
 * <ul>
 *   <li>请求路径末段是动作词（audit/post/close...）→ {@code 模块:动作}</li>
 *   <li>否则按 HTTP 方法：GET→query POST→add PUT→edit DELETE→remove</li>
 * </ul>
 * 路径前缀 {@code /pa/order} 对应权限前缀 {@code pa:order}。
 */
public final class PermissionAnnotator {

    /** 无需权限控制的路径 */
    private static final List<String> SKIP_PREFIXES = List.of(
            "/auth", "/doc.html", "/swagger-ui", "/v3/api-docs", "/error", "/actuator");

    /** 已知动作词 -> 权限动作 */
    private static final List<String> KNOWN_ACTIONS = List.of(
            "audit", "unaudit", "submit", "post", "unpost", "void", "red-flush", "redFlush",
            "close", "open", "approve", "reject", "transfer", "countersign", "rollback",
            "remove", "delete", "export", "import", "add", "create", "save", "edit", "update",
            "execute", "run", "release", "generate", "publish", "assign", "resetPwd");

    /** 一个方法块：从注解开始到 public 签名结束 */
    private static final Pattern METHOD = Pattern.compile(
            "((?:[ \\t]*@\\w+(?:\\([^)]*\\))?[ \\t]*\\r?\\n)+)([ \\t]*public[ \\t]+[\\w<>,\\.\\[\\] \\t]+?[ \\t]+\\w+[ \\t]*\\()",
            Pattern.MULTILINE | Pattern.DOTALL);

    private static final Pattern MAPPING = Pattern.compile("@(\\w+)Mapping(?:\\(\"([^\"]*)\"\\))?");

    public static void main(String[] args) throws IOException {
        Path root = Path.of(args[0]);
        int touched = 0;
        for (Path file : listControllers(root)) {
            String original = Files.readString(file, StandardCharsets.UTF_8);
            String updated = annotate(original);
            if (!updated.equals(original)) {
                Files.writeString(file, updated, StandardCharsets.UTF_8);
                touched++;
                System.out.println("annotated: " + root.relativize(file));
            }
        }
        System.out.println("Total annotated files: " + touched);
    }

    private static String annotate(String source) {
        Matcher classMatcher = Pattern.compile("public class (\\w+Controller)\\b").matcher(source);
        if (!classMatcher.find()) {
            return source;
        }
        Matcher baseMatcher = Pattern.compile("@RequestMapping\\(\"([^\"]*)\"\\)").matcher(source);
        if (!baseMatcher.find()) {
            return source;
        }
        String basePath = baseMatcher.group(1);
        if (SKIP_PREFIXES.stream().anyMatch(basePath::startsWith)) {
            return source;
        }
        String permPrefix = permissionPrefix(basePath);
        String logModule = logModule(basePath);

        StringBuilder out = new StringBuilder();
        Matcher m = METHOD.matcher(source);
        int last = 0;
        while (m.find()) {
            String annotations = m.group(1);
            String signature = m.group(2);

            if (annotations.contains("@RequiresPermission")) {
                out.append(source, last, m.end());
                last = m.end();
                continue;
            }

            String action = deriveAction(annotations, signature);
            if (action == null) {
                out.append(source, last, m.end());
                last = m.end();
                continue;
            }

            out.append(source, last, m.start());
            out.append(annotations);
            out.append("    @RequiresPermission(\"").append(permPrefix).append(':').append(action).append("\")\n");
            out.append("    @OperLog(module = \"").append(logModule).append("\")\n");
            out.append(signature);
            last = m.end();
        }
        out.append(source.substring(last));

        String output = out.toString();
        if (!output.contains("import com.erp.security.annotation.RequiresPermission;")) {
            output = output.replace("import lombok.RequiredArgsConstructor;",
                    "import com.erp.security.annotation.RequiresPermission;\n"
                            + "import com.erp.system.annotation.OperLog;\n"
                            + "import lombok.RequiredArgsConstructor;");
        }
        return output;
    }

    /**
     * 从方法的映射注解推导权限动作。
     */
    private static String deriveAction(String annotations, String signature) {
        Matcher mapping = MAPPING.matcher(annotations);
        while (mapping.find()) {
            String verb = mapping.group(1);
            String path = mapping.group(2) == null ? "" : mapping.group(2);
            // 路径末段为动作词时优先使用
            if (!path.isEmpty()) {
                int idx = path.lastIndexOf('/');
                if (idx >= 0 && idx < path.length() - 1) {
                    String last = path.substring(idx + 1);
                    for (String action : KNOWN_ACTIONS) {
                        if (last.equalsIgnoreCase(action)) {
                            return normalize(action);
                        }
                    }
                }
            }
            switch (verb) {
                case "Get":
                    return "query";
                case "Post":
                    return "add";
                case "Put":
                    return "edit";
                case "Delete":
                    return "remove";
                default:
                    return null;
            }
        }
        return null;
    }

    /**
     * 动作名归一化为权限码风格。
     */
    private static String normalize(String action) {
        if (action.equals("delete")) {
            return "remove";
        }
        if (action.equals("resetPwd")) {
            return "resetPwd";
        }
        if (action.equals("red-flush")) {
            return "redFlush";
        }
        return action;
    }

    /**
     * /pa/order → pa:order
     */
    private static String permissionPrefix(String basePath) {
        String path = basePath.startsWith("/") ? basePath.substring(1) : basePath;
        String[] parts = path.split("/");
        if (parts.length >= 2) {
            return parts[0] + ":" + parts[1];
        }
        return parts.length == 1 && !parts[0].isEmpty() ? parts[0] : "system";
    }

    private static String logModule(String basePath) {
        String path = basePath.startsWith("/") ? basePath.substring(1) : basePath;
        return path.isEmpty() ? "system" : path.split("/")[0];
    }

    private static List<Path> listControllers(Path root) throws IOException {
        List<Path> files = new ArrayList<>();
        try (var stream = Files.walk(root)) {
            stream.filter(p -> p.toString().endsWith("Controller.java")).forEach(files::add);
        }
        return files;
    }
}








