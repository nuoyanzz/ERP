package com.erp.tools;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 将 schema.sql 中的裸 CREATE INDEX 转换为幂等的存储过程调用，
 * 使 schema.sql 可重复执行。
 */
public final class SchemaConverter {

    private static final Pattern CREATE_INDEX = Pattern.compile(
            "^CREATE (UNIQUE )?INDEX\\s+(\\w+)\\s+ON\\s+(\\w+)\\s+\\((.+?)\\);\\s*$",
            Pattern.MULTILINE | Pattern.DOTALL);

    public static void main(String[] args) throws Exception {
        Path path = Path.of(args[0]);
        String sql = Files.readString(path, StandardCharsets.UTF_8);

        Matcher m = CREATE_INDEX.matcher(sql);
        StringBuffer sb = new StringBuffer();
        int count = 0;
        while (m.find()) {
            boolean unique = m.group(1) != null;
            String index = m.group(2);
            String table = m.group(3);
            String columns = m.group(4).replaceAll("\\s+", " ").trim();
            String proc = unique ? "erp_add_uniqueindex" : "erp_add_index";
            m.appendReplacement(sb, Matcher.quoteReplacement(
                    "CALL " + proc + "('" + table + "', '" + index + "', '" + columns + "');"));
            count++;
        }
        m.appendTail(sb);
        Files.writeString(path, sb.toString(), StandardCharsets.UTF_8);
        System.out.println("CREATE INDEX converted: " + count);
    }
}








