package ch.softappeal.yass.remote

import ch.softappeal.yass.*
import kotlin.test.*

private abstract class Role : Services(SimpleMethodMapperFactory)

class ContractIdTest {
    @Test
    fun contractId() {
        val contractId = contractId<Calculator>(123, SimpleMethodMapperFactory)
        assertEquals(123, contractId.id)
        assertSame(Calculator::class.java, contractId.contract)
    }

    @Test
    fun javaContractId() {
        val contractId = contractId(JavaCalculator::class.java, 123, SimpleMethodMapperFactory)
        assertEquals(123, contractId.id)
        assertSame(JavaCalculator::class.java, contractId.contract)
    }

    @Test
    fun okServices() {
        class Initiator : Role() {
            val calculator0 = contractId<Calculator>(0)
            val calculator1 = contractId<Calculator>(1)
        }

        val initiator = Initiator()
        assertEquals(0, initiator.calculator0.id)
        assertSame(Calculator::class.java, initiator.calculator0.contract)
        assertEquals(1, initiator.calculator1.id)
        assertSame(Calculator::class.java, initiator.calculator1.contract)
    }

    @Test
    fun duplicatedServices() {
        class Initiator : Role() {
            val calculator0 = contractId<Calculator>(0)
            val calculator1 = contractId<Calculator>(0)
        }
        assertEquals(
            "service with id 0 already added",
            assertFailsWith<IllegalArgumentException> { Initiator() }.message
        )
    }
}
