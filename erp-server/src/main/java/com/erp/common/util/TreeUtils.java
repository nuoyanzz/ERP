package com.erp.common.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class TreeUtils {

    private TreeUtils() {
    }

    public static <T> List<T> buildTree(List<T> nodes,
                                        Function<? super T, Long> idGetter,
                                        Function<? super T, Long> parentIdGetter,
                                        BiConsumer<? super T, List<T>> childrenSetter) {
        Map<Long, List<T>> byParent = nodes.stream()
                .collect(Collectors.groupingBy(node -> {
                    Long parentId = parentIdGetter.apply(node);
                    return parentId == null ? 0L : parentId;
                }));
        List<T> roots = new ArrayList<>();
        for (T node : nodes) {
            childrenSetter.accept(node, byParent.getOrDefault(idGetter.apply(node), new ArrayList<>()));
            Long parentId = parentIdGetter.apply(node);
            if (parentId == null || parentId == 0L) {
                roots.add(node);
            }
        }
        return roots;
    }
}








