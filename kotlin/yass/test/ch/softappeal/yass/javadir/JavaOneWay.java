package ch.softappeal.yass.javadir;

import ch.softappeal.yass.remote.OneWay;

public interface JavaOneWay {

    @OneWay
    void a();

    void b();

}
