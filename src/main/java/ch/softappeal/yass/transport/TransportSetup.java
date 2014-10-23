package ch.softappeal.yass.transport;

import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.session.SessionSetup;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.util.Check;

import java.util.concurrent.Executor;

public abstract class TransportSetup extends SessionSetup {

  public final Serializer packetSerializer;

  protected TransportSetup(final Server server, final Executor requestExecutor, final Serializer packetSerializer) {
    super(server, requestExecutor);
    this.packetSerializer = Check.notNull(packetSerializer);
  }

}
