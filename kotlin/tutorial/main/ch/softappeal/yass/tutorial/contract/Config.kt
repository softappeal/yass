package ch.softappeal.yass.tutorial.contract

import ch.softappeal.yass.remote.Services
import ch.softappeal.yass.remote.SimpleMethodMapperFactory
import ch.softappeal.yass.serialize.fast.IntSerializer
import ch.softappeal.yass.serialize.fast.simpleFastSerializer
import ch.softappeal.yass.transport.messageSerializer
import ch.softappeal.yass.transport.packetSerializer
import ch.softappeal.yass.tutorial.contract.generic.GenericEchoService
import ch.softappeal.yass.tutorial.contract.generic.Pair
import ch.softappeal.yass.tutorial.contract.generic.PairBoolBool
import ch.softappeal.yass.tutorial.contract.generic.Triple
import ch.softappeal.yass.tutorial.contract.generic.TripleWrapper
import ch.softappeal.yass.tutorial.contract.instrument.Bond
import ch.softappeal.yass.tutorial.contract.instrument.InstrumentService
import ch.softappeal.yass.tutorial.contract.instrument.stock.Stock

val ContractSerializer = simpleFastSerializer(
    ch.softappeal.yass.generate.ts.baseTypeSerializers(
        IntSerializer,
        ExpirationSerializer
    ),
    listOf(
        PriceKind::class.java,
        Price::class.java,
        Stock::class.java,
        Bond::class.java,
        SystemException::class.java,
        UnknownInstrumentsException::class.java,
        Pair::class.java,
        PairBoolBool::class.java,
        Triple::class.java,
        TripleWrapper::class.java
    )
)

val PyContractSerializer = simpleFastSerializer(
    ch.softappeal.yass.generate.py.baseTypeSerializers(
        IntSerializer,
        ExpirationSerializer
    ),
    listOf(
        PriceKind::class.java,
        Price::class.java,
        Stock::class.java,
        Bond::class.java,
        SystemException::class.java,
        UnknownInstrumentsException::class.java
    ),
    listOf(
        Node::class.java
    )
)

val MessageSerializer = messageSerializer(ContractSerializer)
val PacketSerializer = packetSerializer(MessageSerializer)

abstract class Role internal constructor() : Services(SimpleMethodMapperFactory)

class InitiatorClass : Role() { // to be implemented by initiator
    @JvmField
    val priceListener = contractId<PriceListener>(0)
    @JvmField
    val echoService = contractId<EchoService>(1)
}

val Initiator = InitiatorClass()

class AcceptorClass : Role() { // to be implemented by acceptor
    @JvmField
    val priceEngine = contractId<PriceEngine>(0)
    @JvmField
    val instrumentService = contractId<InstrumentService>(1)
    @JvmField
    val echoService = contractId<EchoService>(2)
    @JvmField
    val genericEchoService = contractId<GenericEchoService>(3)
}

val Acceptor = AcceptorClass()

class PyAcceptorClass : Role() { // to be implemented by acceptor
    @JvmField
    val priceEngine = contractId<PriceEngine>(0)
    @JvmField
    val instrumentService = contractId<InstrumentService>(1)
    @JvmField
    val echoService = contractId<EchoService>(2)
}

val PyAcceptor = PyAcceptorClass()
