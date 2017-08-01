package test;

import org.apache.catalina.startup.Tomcat;

import java.io.File;

/*
 * $note: Doesn't work, see http://stackoverflow.com/questions/33381420/tomcat-throws-the-remote-endpoint-was-in-state-binary-full-writing-when.
 */
public final class TomcatContainer {

    public static void main(final String... args) throws Exception {
        final Tomcat tomcat = new Tomcat();
        tomcat.setPort(8080);
        tomcat.addWebapp("", new File("/").getAbsolutePath());
        tomcat.start();
        tomcat.getServer().await();
    }

}
