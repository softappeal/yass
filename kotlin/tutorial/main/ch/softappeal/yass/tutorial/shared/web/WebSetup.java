package ch.softappeal.yass.tutorial.shared.web;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static ch.softappeal.yass.ThreadFactoryKt.getStdErr;
import static ch.softappeal.yass.ThreadFactoryKt.namedThreadFactory;

public abstract class WebSetup {

    public static final String HOST = "0.0.0.0";
    public static final int PORT = 9090;
    public static final String WS_PATH = "/ws";
    public static final String XHR_PATH = "/xhr";
    protected static final String WEB_PATH = "ts";

    public static final Executor DISPATCH_EXECUTOR =
        Executors.newCachedThreadPool(namedThreadFactory("dispatchExecutor", getStdErr()));

}
