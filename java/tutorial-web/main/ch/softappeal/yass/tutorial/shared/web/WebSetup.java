package ch.softappeal.yass.tutorial.shared.web;

import ch.softappeal.yass.Exceptions;
import ch.softappeal.yass.NamedThreadFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public abstract class WebSetup {

    public static final String HOST = "0.0.0.0";
    public static final int PORT = 9090;
    public static final String WS_PATH = "/ws";
    public static final String XHR_PATH = "/xhr";
    protected static final String WEB_PATH = "ts";

    public static final Executor DISPATCH_EXECUTOR = Executors.newCachedThreadPool(new NamedThreadFactory("dispatchExecutor", Exceptions.STD_ERR));

}
