package com.erp.tools;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 开发期 SQL 执行工具：用于验证 schema/migration 脚本在真实 MySQL 上可执行。
 * 用法：java com.erp.tools.SqlRunner <url> <user> <pass> <sqlFile>
 */
public final class SqlRunner {

    public static void main(String[] args) throws Exception {
        String url = args[0];
        String user = args[1];
        String pass = args[2];

        // --drop 模式：重置数据库，用于验证全量初始化脚本
        if (args.length == 4 && "--drop".equals(args[3])) {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String serverUrl = url.replace("/erp?", "/?");
            try (Connection conn = DriverManager.getConnection(serverUrl, user, pass);
                 Statement st = conn.createStatement()) {
                st.execute("DROP DATABASE IF EXISTS erp");
                st.execute("CREATE DATABASE erp DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
                System.out.println("数据库已重置");
            }
            return;
        }

        Path sqlFile = Path.of(args[3]);
        String sql = Files.readString(sqlFile);

        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            System.out.println("Connected: " + conn.getMetaData().getDatabaseProductVersion());
            List<String> statements = split(sql);
            System.out.println("Statements: " + statements.size());
            int ok = 0;
            List<String> failures = new ArrayList<>();
            for (String stmt : statements) {
                if (stmt.trim().isEmpty()) continue;
                try (Statement st = conn.createStatement()) {
                    st.execute(stmt);
                    ok++;
                } catch (Exception e) {
                    failures.add(firstLine(stmt) + "  ==>  " + e.getMessage());
                }
            }
            System.out.println("OK=" + ok + " FAILED=" + failures.size());
            for (String f : failures) {
                System.out.println("[FAIL] " + f);
            }
        }
    }

    private static String firstLine(String s) {
        int i = s.indexOf('\n');
        String line = i > 0 ? s.substring(0, i) : s;
        return line.length() > 150 ? line.substring(0, 150) + "..." : line;
    }

    /** 按分号切分 SQL，跳过注释行；支持 DELIMITER 指令（存储过程体内部的分号不切分）。 */
    private static List<String> split(String sql) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingle = false;
        boolean inDouble = false;
        boolean inBacktick = false;
        String delimiter = ";";
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);

            // 处理 DELIMITER 指令
            if (c == 'D' && current.toString().trim().isEmpty() && startsWithIgnoreCase(sql, i, "DELIMITER")) {
                int lineEnd = sql.indexOf('\n', i);
                if (lineEnd < 0) lineEnd = sql.length();
                String newDelim = sql.substring(i + "DELIMITER".length(), lineEnd).trim();
                if (!newDelim.isEmpty()) {
                    delimiter = newDelim;
                }
                i = lineEnd;
                current.setLength(0);
                continue;
            }

            if (c == '\'' && !inDouble && !inBacktick) inSingle = !inSingle;
            else if (c == '"' && !inSingle && !inBacktick) inDouble = !inDouble;
            else if (c == '`' && !inSingle && !inDouble) inBacktick = !inBacktick;

            if (!inSingle && !inDouble && !inBacktick && sql.startsWith(delimiter, i)) {
                String stmt = current.toString();
                if (!isCommentOnly(stmt)) result.add(stmt);
                current.setLength(0);
                i += delimiter.length() - 1;
            } else {
                current.append(c);
            }
        }
        if (!isCommentOnly(current.toString())) result.add(current.toString());
        return result;
    }

    private static boolean startsWithIgnoreCase(String text, int index, String prefix) {
        return text.regionMatches(true, index, prefix, 0, prefix.length());
    }

    private static boolean isCommentOnly(String stmt) {
        StringBuilder sb = new StringBuilder();
        for (String line : stmt.split("\n")) {
            String t = line.trim();
            if (t.startsWith("--") || t.startsWith("#")) continue;
            sb.append(line).append('\n');
        }
        return sb.toString().trim().isEmpty();
    }
}








