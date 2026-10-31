package com.erp.tools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 开发期数据库检查工具：执行查询并打印结果。
 */
public final class DbInspect {

    public static void main(String[] args) throws Exception {
        String url = args[0];
        String user = args[1];
        String pass = args[2];
        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            for (int i = 3; i < args.length; i++) {
                run(conn, args[i]);
            }
        }
    }

    private static void run(Connection conn, String sql) throws Exception {
        System.out.println("=== " + sql + " ===");
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();
            StringBuilder header = new StringBuilder();
            for (int i = 1; i <= cols; i++) {
                if (i > 1) header.append(" | ");
                header.append(md.getColumnLabel(i));
            }
            System.out.println(header);
            System.out.println("-".repeat(Math.min(120, header.length())));
            int count = 0;
            while (rs.next()) {
                StringBuilder row = new StringBuilder();
                for (int i = 1; i <= cols; i++) {
                    if (i > 1) row.append(" | ");
                    Object v = rs.getObject(i);
                    row.append(v == null ? "" : String.valueOf(v));
                }
                System.out.println(row);
                count++;
            }
            System.out.println("(" + count + " rows)");
            System.out.println();
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
            System.out.println();
        }
    }
}








