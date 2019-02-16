package ch.softappeal.yass.ktutorial.session

import ch.softappeal.yass.remote.*
import ch.softappeal.yass.serialize.fast.*
import ch.softappeal.yass.transport.*

private val ContractSerializer = sSimpleFastSerializer(
    listOf(
        SIntSerializer,
        SStringSerializer
    ),
    listOf(
        DivisionByZeroException::class,
        WeatherType::class,
        Weather::class
    )
)

val PacketSerializer = sPacketSerializer(sMessageSerializer(ContractSerializer))

abstract class Role : Services(SimpleMethodMapperFactory)

class InitiatorRole : Role() {
    val echoService = contractId<EchoService>(0)
    val weatherListener = contractId<WeatherListener>(1)
}

class AcceptorRole : Role() {
    val echoService = contractId<EchoService>(0)
    val calculator = contractId<Calculator>(1)
}

val Initiator = InitiatorRole()
val Acceptor = AcceptorRole()
