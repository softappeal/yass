package ch.softappeal.yass.tutorial.datagram;

import ch.softappeal.yass.remote.*;
import ch.softappeal.yass.tutorial.contract.*;

import java.net.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;

public final class PriceSender {

    public static final String GROUP_BID = "239.1.1.1";
    public static final String GROUP_ASK = "239.1.1.2";
    public static final int PORT = 8989;

    public static void main(final String... args) throws Exception {
        final DatagramChannel channel = DatagramChannel.open();
        final Client clientBid =
            DatagramTransport.client(Config.MESSAGE_SERIALIZER, channel, new InetSocketAddress(GROUP_BID, PORT));
        final Client clientAsk =
            DatagramTransport.client(Config.MESSAGE_SERIALIZER, channel, new InetSocketAddress(GROUP_ASK, PORT));
        final PriceListener priceListenerBid = clientBid.proxy(Config.INITIATOR.priceListener);
        final PriceListener priceListenerAsk = clientAsk.proxy(Config.INITIATOR.priceListener);
        for (int value = 1; true; value++) {
            System.out.println("sending BID and ASK: " + value);
            priceListenerBid.newPrices(Collections.singletonList(new Price(123, value, PriceKind.BID)));
            priceListenerAsk.newPrices(Collections.singletonList(new Price(123, value, PriceKind.ASK)));
            TimeUnit.MILLISECONDS.sleep(100);
        }
    }

}
