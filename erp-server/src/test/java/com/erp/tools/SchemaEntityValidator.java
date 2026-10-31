package com.erp.tools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 校验实体字段与数据库表结构的匹配情况。
 * <p>
 * 逐个实体验证很慢，且容易遗漏。这里一次性扫描所有以 {@code @TableName} 标注的实体，
 * 与数据库实际列做比对，找出「实体有字段但表无对应列」的不匹配。
 */
public final class SchemaEntityValidator {

    public static void main(String[] args) throws Exception {
        String url = args[0];
        String user = args[1];
        String pass = args[2];
        String entityRoot = args[3];

        Map<String, String> entityTables = scanEntities(entityRoot);
        System.out.println("扫描到实体: " + entityTables.size());

        List<String> problems = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            for (Map.Entry<String, String> entry : entityTables.entrySet()) {
                String entityClass = entry.getKey();
                String tableName = entry.getValue();
                Set<String> columns = loadColumns(conn, tableName);
                if (columns == null) {
                    problems.add("表不存在: " + tableName + " (实体 " + entityClass + ")");
                    continue;
                }
                List<String> missing = checkEntityFields(entityClass, columns);
                if (!missing.isEmpty()) {
                    problems.add(tableName + " 缺少列: " + String.join(", ", missing)
                            + "  (实体 " + entityClass + ")");
                }
            }
        }

        if (problems.isEmpty()) {
            System.out.println("=== 全部实体与表结构匹配 ===");
        } else {
            System.out.println("=== 发现 " + problems.size() + " 处不匹配 ===");
            problems.forEach(p -> System.out.println("  [X] " + p));
        }
    }

    /**
     * 扫描源码目录，找 @TableName 注解与实体字段。
     */
    private static Map<String, String> scanEntities(String root) throws Exception {
        Map<String, String> result = new LinkedHashMap<>();
        java.nio.file.Path rootPath = java.nio.file.Path.of(root);
        if (!java.nio.file.Files.exists(rootPath)) {
            return result;
        }
        try (var stream = java.nio.file.Files.walk(rootPath)) {
            stream.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                try {
                    String content = java.nio.file.Files.readString(p);
                    java.util.regex.Matcher m = java.util.regex.Pattern
                            .compile("@TableName\\(\"([^\"]+)\"\\)").matcher(content);
                    if (m.find()) {
                        result.put(p.toString(), m.group(1));
                    }
                } catch (Exception ignored) {
                    // 读取失败跳过
                }
            });
        }
        return result;
    }

    /**
     * 用反射加载实体类，校验字段对应的列是否存在。
     */
    private static List<String> checkEntityFields(String entityFile, Set<String> columns) {
        List<String> missing = new ArrayList<>();
        String className = extractClassName(entityFile);
        if (className == null) {
            return missing;
        }
        try {
            Class<?> clazz = Class.forName(className, false,
                    Thread.currentThread().getContextClassLoader());
            Class<?> current = clazz;
            while (current != null && current != Object.class) {
                for (java.lang.reflect.Field field : current.getDeclaredFields()) {
                    if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                        continue;
                    }
                    com.baomidou.mybatisplus.annotation.TableField ann =
                            field.getAnnotation(com.baomidou.mybatisplus.annotation.TableField.class);
                    if (ann != null && "logic".equals(ann.value())) {
                        continue;
                    }
                    String column = ann != null && !ann.value().isEmpty()
                            ? ann.value() : toSnakeCase(field.getName());
                    if (!columns.contains(column)) {
                        missing.add(column);
                    }
                }
                current = current.getSuperclass();
            }
        } catch (ClassNotFoundException e) {
            // 实体未编译，跳过
        }
        return missing;
    }

    private static String extractClassName(String file) {
        try {
            String content = java.nio.file.Files.readString(java.nio.file.Path.of(file));
            java.util.regex.Matcher pkg = java.util.regex.Pattern
                    .compile("^package\\s+([\\w.]+);", java.util.regex.Pattern.MULTILINE).matcher(content);
            java.util.regex.Matcher cls = java.util.regex.Pattern
                    .compile("public\\s+class\\s+(\\w+)").matcher(content);
            if (pkg.find() && cls.find()) {
                return pkg.group(1) + "." + cls.group(1);
            }
        } catch (Exception ignored) {
            // 忽略
        }
        return null;
    }

    private static String toSnakeCase(String name) {
        return name.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
    }

    private static Set<String> loadColumns(Connection conn, String tableName) {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT COLUMN_NAME FROM information_schema.COLUMNS "
                             + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = '"
                             + tableName.replace("'", "") + "'")) {
            Set<String> columns = new java.util.LinkedHashSet<>();
            while (rs.next()) {
                columns.add(rs.getString(1).toLowerCase());
            }
            return columns.isEmpty() ? null : columns;
        } catch (Exception e) {
            return null;
        }
    }
}








