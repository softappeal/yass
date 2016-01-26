package ch.softappeal.yass.tutorial.dg;

import ch.softappeal.yass.core.remote.Client;
import ch.softappeal.yass.tutorial.contract.Config;
import ch.softappeal.yass.tutorial.contract.Price;
import ch.softappeal.yass.tutorial.contract.PriceKind;
import ch.softappeal.yass.tutorial.contract.PriceListener;

import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public final class PriceSender {

    public static final String GROUP = "239.1.1.1";
    public static final int PORT = 8989;

    public static void main(final String... args) throws Exception {
        final DatagramChannel channel = DatagramChannel.open().
            connect(new InetSocketAddress(GROUP, PORT));
        final Client client = DatagramTransport.client(Config.MESSAGE_SERIALIZER, channel);
        final PriceListener priceListener = client.proxy(Config.INITIATOR.priceListener);
        for (int value = 1; true; value++) {
            System.out.println(value);
            priceListener.newPrices(Collections.singletonList(new Price(123, value, PriceKind.ASK)));
            TimeUnit.MILLISECONDS.sleep(100);
        }
    }

}
