package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.core.remote.ContractId;
import ch.softappeal.yass.core.remote.Services;
import ch.softappeal.yass.core.remote.SimpleMethodMapper;
import ch.softappeal.yass.generate.PythonGenerator;
import ch.softappeal.yass.generate.TypeScriptGenerator;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.fast.BaseTypeHandlers;
import ch.softappeal.yass.serialize.fast.FastSerializer;
import ch.softappeal.yass.serialize.fast.SimpleFastSerializer;
import ch.softappeal.yass.transport.MessageSerializer;
import ch.softappeal.yass.tutorial.contract.generic.GenericEchoService;
import ch.softappeal.yass.tutorial.contract.generic.Pair;
import ch.softappeal.yass.tutorial.contract.generic.PairBoolBool;
import ch.softappeal.yass.tutorial.contract.generic.Triple;
import ch.softappeal.yass.tutorial.contract.generic.TripleWrapper;
import ch.softappeal.yass.tutorial.contract.instrument.Bond;
import ch.softappeal.yass.tutorial.contract.instrument.InstrumentService;
import ch.softappeal.yass.tutorial.contract.instrument.stock.Stock;
import ch.softappeal.yass.tutorial.contract.instrument.stock.python.PythonBond;
import ch.softappeal.yass.tutorial.contract.instrument.stock.python.PythonStock;
import ch.softappeal.yass.util.Instantiators;
import ch.softappeal.yass.util.unsupported.UnsupportedInstantiators;

import java.util.List;

public final class Config {

    /**
     * @see Price#Price()
     */
    public static final FastSerializer CONTRACT_SERIALIZER = new SimpleFastSerializer(
        UnsupportedInstantiators.UNSAFE,
        TypeScriptGenerator.baseTypeHandlers( // note: order is important; id's must match with TypeScript implementations
            BaseTypeHandlers.INTEGER,         // TypeScriptGenerator.FIRST_DESC_ID
            Expiration.TYPE_HANDLER           // TypeScriptGenerator.FIRST_DESC_ID + 1
        ),
        List.of(
            PriceKind.class,
            Price.class,
            Stock.class,
            Bond.class,
            SystemException.class,
            UnknownInstrumentsException.class,
            Node.class,
            Pair.class,
            PairBoolBool.class,
            Triple.class,
            TripleWrapper.class
        )
    );

    public static final FastSerializer PY_CONTRACT_SERIALIZER = new SimpleFastSerializer(
        Instantiators.NOARG,
        PythonGenerator.baseTypeHandlers( // note: order is important; id's must match with Python implementations
            BaseTypeHandlers.INTEGER,     // PythonGenerator.FIRST_DESC_ID
            Expiration.TYPE_HANDLER       // PythonGenerator.FIRST_DESC_ID + 1
        ),
        List.of(
            PriceKind.class,
            Price.class,
            Stock.class,
            Bond.class,
            SystemException.class,
            UnknownInstrumentsException.class,
            PythonBond.class,
            PythonStock.class
        ),
        List.of(
            Node.class
        )
    );

    public static final Serializer MESSAGE_SERIALIZER = new MessageSerializer(CONTRACT_SERIALIZER);

    private abstract static class Role extends Services {
        Role() {
            super(SimpleMethodMapper.FACTORY);
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

    public static final class PyAcceptor extends Role { // to be implemented by acceptor
        public final ContractId<PriceEngine> priceEngine = contractId(PriceEngine.class, 0);
        public final ContractId<InstrumentService> instrumentService = contractId(InstrumentService.class, 1);
        public final ContractId<EchoService> echoService = contractId(EchoService.class, 2);
    }

    public static final PyAcceptor PY_ACCEPTOR = new PyAcceptor();

}
