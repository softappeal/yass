package ch.softappeal.yass.transport.ws.container;

// import org.apache.catalina.startup.Tomcat;

// import java.io.File;

/**
 * note: Doesn't work, see
 * http://stackoverflow.com/questions/33381420/tomcat-throws-the-remote-endpoint-was-in-state-binary-full-writing-when.
 */
public final class TomcatContainer {

    public static void main(final String... args) {
        /*
        final var tomcat = new Tomcat();
        tomcat.setPort(8080);
        tomcat.addWebapp("", new File("/").getAbsolutePath());
        tomcat.start();
        tomcat.getServer().await();
        */
    }

}

/*

dependencies {
    tomcatCompile 'org.apache.tomcat.embed:tomcat-embed-websocket:8.5.24'
    tomcatCompile 'org.apache.tomcat.embed:tomcat-embed-logging-juli:8.5.2'
    tomcatCompile 'org.apache.tomcat:jasper:6.0.53'
}

*/
