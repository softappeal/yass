package ch.softappeal.yass.tutorial.datagram;

import ch.softappeal.yass.core.remote.Server;
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
        final SimpleTransportSetup setup = new SimpleTransportSetup(
            Config.MESSAGE_SERIALIZER,
            new Server(
                Config.INITIATOR.priceListener.service(new PriceListener() {
                    @Override public void newPrices(final List<Price> prices) {
                        final Price price = prices.get(0);
                        System.out.println("received " + price.kind + ": " + price.value);
                    }
                })
            )
        );
        final DatagramChannel channel = DatagramChannel.open(StandardProtocolFamily.INET)
            .setOption(StandardSocketOptions.SO_REUSEADDR, true)
            .bind(new InetSocketAddress(PriceSender.PORT));
        final NetworkInterface networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
        channel.join(InetAddress.getByName(PriceSender.GROUP_BID), networkInterface);
        channel.join(InetAddress.getByName(PriceSender.GROUP_ASK), networkInterface);
        while (true) {
            DatagramTransport.invoke(setup, channel, 128);
        }
    }

}
