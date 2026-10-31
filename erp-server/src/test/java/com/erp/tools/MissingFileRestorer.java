package com.erp.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 找出当前 HEAD 缺失但存在于历史提交中的文件，并从最新提交恢复。
 * <p>
 * 用于修复 rebase 过程中意外丢失的文件。
 */
public final class MissingFileRestorer {

    public static void main(String[] args) throws Exception {
        Path repo = Path.of(args[0]);
        List<String> commits = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            commits.add(args[i]);
        }

        // 收集历史中出现过的所有文件及其最新版本所属提交
        Map<String, String> latest = new LinkedHashMap<>();
        for (String commit : commits) {
            Process process = new ProcessBuilder(
                    "git", "ls-tree", "-r", "--name-only", commit)
                    .directory(repo.toFile())
                    .redirectErrorStream(true)
                    .start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            process.waitFor();
            for (String line : output.split("\n")) {
                String path = line.trim();
                if (path.isEmpty()) {
                    continue;
                }
                latest.put(path, commit);
            }
        }

        int restored = 0;
        int missing = 0;
        for (Map.Entry<String, String> entry : latest.entrySet()) {
            String path = entry.getKey();
            if (!path.startsWith("erp-server/src/") && !path.startsWith("erp-web/src/")) {
                continue;
            }
            Path target = repo.resolve(path);
            if (Files.exists(target)) {
                continue;
            }
            missing++;
            Process process = new ProcessBuilder(
                    "git", "checkout", entry.getValue(), "--", path)
                    .directory(repo.toFile())
                    .redirectErrorStream(true)
                    .start();
            process.getInputStream().readAllBytes();
            int code = process.waitFor();
            if (code == 0) {
                restored++;
                System.out.println("  恢复: " + path + "  (来自 " + entry.getValue().substring(0, 7) + ")");
            }
        }
        System.out.println("缺失 " + missing + " 个，已恢复 " + restored + " 个");
    }
}








