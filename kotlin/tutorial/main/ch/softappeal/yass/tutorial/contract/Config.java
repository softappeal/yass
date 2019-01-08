package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.generate.py.*;
import ch.softappeal.yass.generate.ts.*;
import ch.softappeal.yass.remote.*;
import ch.softappeal.yass.serialize.*;
import ch.softappeal.yass.serialize.fast.*;
import ch.softappeal.yass.tutorial.contract.generic.*;
import ch.softappeal.yass.tutorial.contract.instrument.*;
import ch.softappeal.yass.tutorial.contract.instrument.stock.*;

import java.util.*;

import static ch.softappeal.yass.remote.MethodMapperKt.*;
import static ch.softappeal.yass.serialize.fast.BaseTypeSerializersKt.*;
import static ch.softappeal.yass.serialize.fast.FastSerializersKt.*;
import static ch.softappeal.yass.transport.MessageSerializerKt.*;
import static ch.softappeal.yass.transport.PacketSerializerKt.*;

public final class Config {

    public static final FastSerializer CONTRACT_SERIALIZER = jSimpleFastSerializer(
        TypeScriptGeneratorKt.baseTypeSerializers(
            getIntSerializer(),
            Expiration.TYPE_SERIALIZER
        ),
        Arrays.asList(
            PriceKind.class,
            Price.class,
            Stock.class,
            Bond.class,
            SystemException.class,
            UnknownInstrumentsException.class,
            Pair.class,
            PairBoolBool.class,
            Triple.class,
            TripleWrapper.class
        ),
        Collections.emptyList(),
        false
    );

    public static final Serializer MESSAGE_SERIALIZER = messageSerializer(CONTRACT_SERIALIZER);
    public static final Serializer PACKET_SERIALIZER = packetSerializer(MESSAGE_SERIALIZER);

    private abstract static class Role extends Services {
        Role() {
            super(getSimpleMethodMapperFactory());
        }
    }

    public static final class Initiator extends Role { // to be implemented by initiator
        public final ContractId<PriceListener> priceListener = contractId(PriceListener.class, 0);
        public final ContractId<EchoService> echoService = contractId(EchoService.class, 1);
    }

    public static final class Acceptor extends Role { // to be implemented by acceptor
        public final ContractId<PriceEngine> priceEngine = contractId(PriceEngine.class, 0);
        public final ContractId<InstrumentService> instrumentService = contractId(InstrumentService.class, 1);
        public final ContractId<EchoService> echoService = contractId(EchoService.class, 2);
        public final ContractId<GenericEchoService> genericEchoService = contractId(GenericEchoService.class, 3);
    }

    public static final Initiator INITIATOR = new Initiator();
    public static final Acceptor ACCEPTOR = new Acceptor();

    public static final FastSerializer PY_CONTRACT_SERIALIZER = jSimpleFastSerializer(
        PythonGeneratorKt.baseTypeSerializers(
            getIntSerializer(),
            Expiration.TYPE_SERIALIZER
        ),
        Arrays.asList(
            PriceKind.class,
            Price.class,
            Stock.class,
            Bond.class,
            SystemException.class,
            UnknownInstrumentsException.class
        ),
        Arrays.asList(
            Node.class
        ),
        false
    );

    public static final class PyAcceptor extends Role {
        public final ContractId<PriceEngine> priceEngine = contractId(PriceEngine.class, 0);
        public final ContractId<InstrumentService> instrumentService = contractId(InstrumentService.class, 1);
        public final ContractId<EchoService> echoService = contractId(EchoService.class, 2);
    }

    public static final PyAcceptor PY_ACCEPTOR = new PyAcceptor();

}
