package com.erp.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * 提交分组工具：按文件的最后修改时间，把待提交文件划分到对应的历史阶段。
 * <p>
 * 用于重建与真实开发过程一致的历史提交——先按时间切分，再按模块细分。
 */
public final class CommitPartitioner {

    /**
     * 按最后修改时间把文件分组。
     *
     * @param root    仓库根目录
     * @param cutoffs 各阶段的时间分界点（升序），最后一个必须是最大值
     * @return 每个阶段对应的文件列表
     */
    public static List<List<String>> partitionByTime(Path root, long... cutoffs) throws IOException {
        List<List<String>> buckets = new ArrayList<>();
        for (int i = 0; i < cutoffs.length; i++) {
            buckets.add(new ArrayList<>());
        }
        try (Stream<Path> stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile).forEach(file -> {
                Path repoRoot = root.toAbsolutePath().normalize();
                Path relative = repoRoot.relativize(file.toAbsolutePath().normalize());
                String rel = relative.toString().replace('\\', '/');
                if (rel.startsWith(".git/")) {
                    return;
                }
                long mtime;
                try {
                    mtime = Files.getLastModifiedTime(file).toMillis();
                } catch (IOException e) {
                    return;
                }
                int index = 0;
                for (int i = 0; i < cutoffs.length; i++) {
                    if (mtime < cutoffs[i]) {
                        index = i;
                        break;
                    }
                    index = i;
                }
                buckets.get(index).add(rel);
            });
        }
        return buckets;
    }

    /**
     * 按路径前缀把文件分组到若干子集合。
     */
    public static List<List<String>> splitByPrefix(List<String> files, String... prefixes) {
        List<List<String>> result = new ArrayList<>();
        for (int i = 0; i < prefixes.length; i++) {
            result.add(new ArrayList<>());
        }
        for (String file : files) {
            int index = prefixes.length - 1;
            for (int i = 0; i < prefixes.length; i++) {
                if (file.startsWith(prefixes[i])) {
                    index = i;
                    break;
                }
            }
            result.get(index).add(file);
        }
        return result;
    }

    /**
     * 读取文件内容（用于检查）。
     */
    public static String read(Path root, String relative) {
        try {
            return Files.readString(root.resolve(relative), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]);
        long day1 = System.currentTimeMillis();
        List<List<String>> buckets = partitionByTime(root, day1);
        for (int i = 0; i < buckets.size(); i++) {
            System.out.println("阶段 " + i + ": " + buckets.get(i).size() + " 个文件");
        }
    }
}








