package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.core.remote.ContractId;
import ch.softappeal.yass.core.remote.Services;
import ch.softappeal.yass.core.remote.SimpleMethodMapper;
import ch.softappeal.yass.generate.Python3Generator;
import ch.softappeal.yass.generate.TypeScriptGenerator;
import ch.softappeal.yass.serialize.FastReflector;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.fast.BaseTypeHandlers;
import ch.softappeal.yass.serialize.fast.FastSerializer;
import ch.softappeal.yass.serialize.fast.SimpleFastSerializer;
import ch.softappeal.yass.transport.MessageSerializer;
import ch.softappeal.yass.tutorial.contract.instrument.Bond;
import ch.softappeal.yass.tutorial.contract.instrument.InstrumentService;
import ch.softappeal.yass.tutorial.contract.instrument.stock.Stock;
import ch.softappeal.yass.tutorial.contract.instrument.stock.python.PythonBond;
import ch.softappeal.yass.tutorial.contract.instrument.stock.python.PythonStock;

import java.util.Arrays;

public final class Config {

    public static final FastSerializer CONTRACT_SERIALIZER = new SimpleFastSerializer(
        FastReflector.FACTORY,
        TypeScriptGenerator.baseTypeHandlers( // note: order is important; id's must match with TypeScript implementations
            BaseTypeHandlers.INTEGER,         // TypeScriptGenerator.FIRST_DESC_ID
            Expiration.TYPE_HANDLER           // TypeScriptGenerator.FIRST_DESC_ID + 1
        ),
        Arrays.asList(
            PriceKind.class,
            Price.class,
            Stock.class,
            Bond.class,
            SystemException.class,
            UnknownInstrumentsException.class
        )
    );

    public static final FastSerializer PY3_CONTRACT_SERIALIZER = new SimpleFastSerializer(
        FastReflector.FACTORY,
        Python3Generator.baseTypeHandlers( // note: order is important; id's must match with Python implementations
            BaseTypeHandlers.INTEGER,      // Python3Generator.FIRST_DESC_ID
            Expiration.TYPE_HANDLER        // Python3Generator.FIRST_DESC_ID + 1
        ),
        Arrays.asList(
            PriceKind.class,
            Price.class,
            Stock.class,
            Bond.class,
            SystemException.class,
            UnknownInstrumentsException.class,
            PythonBond.class,
            PythonStock.class
        ),
        Arrays.asList(
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
    }

    public static final Initiator INITIATOR = new Initiator();
    public static final Acceptor ACCEPTOR = new Acceptor();

}
