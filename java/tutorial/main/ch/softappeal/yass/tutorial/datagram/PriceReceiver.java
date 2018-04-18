package ch.softappeal.yass.tutorial.datagram;

import ch.softappeal.yass.remote.Server;
import ch.softappeal.yass.remote.Service;
import ch.softappeal.yass.transport.SimpleTransportSetup;
import ch.softappeal.yass.tutorial.contract.Config;
import ch.softappeal.yass.tutorial.contract.Price;
import ch.softappeal.yass.tutorial.contract.PriceListener;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.nio.channels.DatagramChannel;
import java.util.List;

public final class PriceReceiver {

    public static void main(final String... args) throws Exception {
        final var setup = new SimpleTransportSetup(
            Config.MESSAGE_SERIALIZER,
            new Server(
                new Service(Config.INITIATOR.priceListener, new PriceListener() {
                    @Override public void newPrices(final List<Price> prices) {
                        final var price = prices.get(0);
                        System.out.println("received " + price.kind + ": " + price.value);
                    }
                })
            )
        );
        final var channel = DatagramChannel.open(StandardProtocolFamily.INET)
            .setOption(StandardSocketOptions.SO_REUSEADDR, true)
            .bind(new InetSocketAddress(PriceSender.PORT));
        final var networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
        channel.join(InetAddress.getByName(PriceSender.GROUP_BID), networkInterface);
        channel.join(InetAddress.getByName(PriceSender.GROUP_ASK), networkInterface);
        while (true) {
            DatagramTransport.invoke(setup, channel, 128);
        }
    }

}
