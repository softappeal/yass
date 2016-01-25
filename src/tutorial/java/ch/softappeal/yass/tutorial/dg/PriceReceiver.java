package ch.softappeal.yass.tutorial.dg;

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
                        System.out.println(prices.get(0).value);
                    }
                })
            )
        );
        final DatagramChannel channel = DatagramChannel.open(StandardProtocolFamily.INET)
            .setOption(StandardSocketOptions.SO_REUSEADDR, true)
            .bind(new InetSocketAddress(PriceSender.PORT));
        channel.join(InetAddress.getByName(PriceSender.GROUP), NetworkInterface.getByInetAddress(InetAddress.getLocalHost()));
        while (true) {
            DatagramTransport.invoke(setup, channel, 128);
        }
    }

}
