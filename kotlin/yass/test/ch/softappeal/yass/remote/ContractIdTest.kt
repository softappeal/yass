package ch.softappeal.yass.remote

import ch.softappeal.yass.JavaCalculator
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.fail

class ContractIdTest {

    @Test
    fun contractId() {
        val contractId = ContractId<Calculator>(123, SimpleMethodMapperFactory)
        assertEquals(123, contractId.id)
        assertSame(Calculator::class.java, contractId.contract)
    }

    @Test
    fun javaContractId() {
        val contractId = ContractId<JavaCalculator>(123, SimpleMethodMapperFactory)
        assertEquals(123, contractId.id)
        assertSame(JavaCalculator::class.java, contractId.contract)
    }

    private abstract class Role : Services(SimpleMethodMapperFactory)

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
        try {
            class Initiator : Role() {
                val calculator0 = contractId<Calculator>(0)
                val calculator1 = contractId<Calculator>(0)
            }
            Initiator()
            fail()
        } catch (e: IllegalArgumentException) {
            assertEquals("service with id 0 already added", e.message)
        }
    }

}
