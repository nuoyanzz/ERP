package com.erp.tools;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 将迁移脚本中的 MySQL 不支持的 DDL 语法转换为幂等的存储过程调用。
 * <p>
 * MySQL 8 不支持 {@code ALTER TABLE ... ADD COLUMN IF NOT EXISTS}，
 * 统一改写为 {@code CALL erp_add_column(...)}。
 */
public final class MigrationConverter {

    private static final Pattern ADD_COLUMN = Pattern.compile(
            "^ALTER TABLE\\s+(\\w+)\\s+ADD COLUMN IF NOT EXISTS\\s+(\\w+)\\s+(.+?);\\s*$",
            Pattern.MULTILINE | Pattern.DOTALL);

    private static final Pattern CREATE_INDEX = Pattern.compile(
            "^CREATE (UNIQUE )?INDEX\\s+(\\w+)\\s+ON\\s+(\\w+)\\s+\\((.+?)\\);\\s*$",
            Pattern.MULTILINE | Pattern.DOTALL);

    public static void main(String[] args) throws Exception {
        Path path = Path.of(args[0]);
        String sql = Files.readString(path, StandardCharsets.UTF_8);

        Matcher m = ADD_COLUMN.matcher(sql);
        StringBuffer sb = new StringBuffer();
        int count = 0;
        while (m.find()) {
            String table = m.group(1);
            String column = m.group(2);
            String definition = m.group(3).replaceAll("\\s+", " ").trim();
            m.appendReplacement(sb, Matcher.quoteReplacement(
                    "CALL erp_add_column('" + table + "', '" + column + "', '" + definition + "');"));
            count++;
        }
        m.appendTail(sb);
        sql = sb.toString();
        System.out.println("ADD COLUMN converted: " + count);

        Matcher m2 = CREATE_INDEX.matcher(sql);
        StringBuffer sb2 = new StringBuffer();
        int count2 = 0;
        while (m2.find()) {
            boolean unique = m2.group(1) != null;
            String index = m2.group(2);
            String table = m2.group(3);
            String columns = m2.group(4).replaceAll("\\s+", " ").trim();
            String proc = unique ? "erp_add_unique_index" : "erp_add_index";
            m2.appendReplacement(sb2, Matcher.quoteReplacement(
                    "CALL " + proc + "('" + table + "', '" + index + "', '" + columns + "');"));
            count2++;
        }
        m2.appendTail(sb2);
        sql = sb2.toString();
        System.out.println("CREATE INDEX converted: " + count2);

        Files.writeString(path, sql, StandardCharsets.UTF_8);
        System.out.println("Written: " + path);
    }
}








