package ch.softappeal.yass.tutorial.datagram;

import ch.softappeal.yass.remote.Server;
import ch.softappeal.yass.transport.ServerTransport;
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

import static ch.softappeal.yass.remote.Kt.service;

public final class PriceReceiver {

    public static void main(final String... args) throws Exception {
        final ServerTransport transport = new ServerTransport(
            new Server(
                service(Config.INITIATOR.priceListener, new PriceListener() {
                    @Override public void newPrices(final List<Price> prices) {
                        final Price price = prices.get(0);
                        System.out.println("received " + price.kind + ": " + price.value);
                    }
                })
            ),
            Config.MESSAGE_SERIALIZER
        );
        final DatagramChannel channel = DatagramChannel.open(StandardProtocolFamily.INET)
            .setOption(StandardSocketOptions.SO_REUSEADDR, true)
            .bind(new InetSocketAddress(PriceSender.PORT));
        final NetworkInterface networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
        channel.join(InetAddress.getByName(PriceSender.GROUP_BID), networkInterface);
        channel.join(InetAddress.getByName(PriceSender.GROUP_ASK), networkInterface);
        while (true) {
            DatagramTransport.invoke(transport, channel, 128);
        }
    }

}
