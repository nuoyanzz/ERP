package com.erp.tools;

import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.io.PrintWriter;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

/**
 * 独立的 JUnit 运行器。用于在不依赖完整 Spring 上下文的情况下运行纯单元测试。
 */
public final class TestRunner {

    public static void main(String[] args) {
        LauncherDiscoveryRequestBuilderHolder holder = new LauncherDiscoveryRequestBuilderHolder();
        var builder = request();
        for (String arg : args) {
            builder.selectors(selectClass(arg));
        }
        LauncherDiscoveryRequest req = builder.build();

        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        launcher.execute(req, listener);

        TestExecutionSummary summary = listener.getSummary();
        PrintWriter writer = new PrintWriter(System.out);
        summary.printTo(writer);
        summary.printFailuresTo(writer, 20);
        writer.flush();

        System.out.println("=== 结果 ===");
        System.out.println("测试总数: " + summary.getTestsFoundCount());
        System.out.println("成功: " + summary.getTestsSucceededCount());
        System.out.println("失败: " + summary.getTestsFailedCount());
        System.out.println("跳过: " + summary.getTestsSkippedCount());
        if (summary.getTestsFailedCount() > 0) {
            System.exit(1);
        }
    }

    private static class LauncherDiscoveryRequestBuilderHolder {
    }
}








