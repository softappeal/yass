package ch.softappeal.yass.tutorial.dg;

import ch.softappeal.yass.core.remote.Client;
import ch.softappeal.yass.tutorial.contract.Config;
import ch.softappeal.yass.tutorial.contract.Price;
import ch.softappeal.yass.tutorial.contract.PriceKind;
import ch.softappeal.yass.tutorial.contract.PriceListener;

import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.StandardSocketOptions;
import java.nio.channels.DatagramChannel;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public final class PriceSender {

    public static final String GROUP = "239.1.1.1";

    public static final int PORT = 8989;

    public static final NetworkInterface NI;
    static {
        try {
            NI = NetworkInterface.getByName("lo");
        } catch (final SocketException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(final String... args) throws Exception {
        final DatagramChannel channel = DatagramChannel.open()
            .setOption(StandardSocketOptions.IP_MULTICAST_IF, NI);
        final Client client = DatagramTransport.client(Config.MESSAGE_SERIALIZER, channel, new InetSocketAddress(GROUP, PORT));
        final PriceListener priceListener = client.proxy(Config.INITIATOR.priceListener);
        for (int value = 1; true; value++) {
            System.out.println(value);
            priceListener.newPrices(Collections.singletonList(new Price(123, value, PriceKind.ASK)));
            TimeUnit.MILLISECONDS.sleep(100);
        }
    }

}
