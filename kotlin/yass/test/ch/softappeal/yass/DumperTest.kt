package ch.softappeal.yass

import ch.softappeal.yass.serialize.createGraph
import ch.softappeal.yass.serialize.createNulls
import ch.softappeal.yass.serialize.createValues
import org.junit.Test
import java.io.PrintWriter
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.util.Date
import java.util.TimeZone
import kotlin.test.assertEquals

private fun dumper(compact: Boolean, graph: Boolean, concreteValueClasses: Set<Class<*>>): Dumper {
    val valueDumper: ValueDumper = { out, value ->
        val type = value.javaClass
        if (
            concreteValueClasses.contains(type) ||
            (Date::class.java === type) ||
            (BigInteger::class.java === type) ||
            (BigDecimal::class.java === type) ||
            (Instant::class.java === type)
        ) out.append(value)
    }
    return if (graph) graphDumper(compact, concreteValueClasses, valueDumper) else treeDumper(compact, valueDumper)
}

class DumperTest {
    @Test
    fun test() {
        val timeZone = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            compareFile("ch/softappeal/yass/DumperTest.dump.txt") { printer ->
                fun print(dumper: Dumper, printer: PrintWriter, cycles: Boolean) {
                    val s = StringBuilder(1024)
                    fun dump(value: Any?) {
                        dumper(s, value).append('\n')
                    }
                    dump(null)
                    dump('c')
                    dump(createNulls())
                    dump(createValues())
                    if (cycles) dump(createGraph())
                    dump(arrayOf<Any>("one", "two", "three"))
                    dump(mapOf(1 to "one", 2 to null, 3 to "three"))
                    printer.append(s)
                }
                print(
                    dumper(false, true, setOf(BigInteger::class.java, BigDecimal::class.java, Instant::class.java)),
                    printer,
                    true
                )
                print(dumper(false, false, emptySet()), printer, false)
                print(dumper(true, true, setOf(BigInteger::class.java, BigDecimal::class.java, Instant::class.java)), printer, true)
                print(dumper(true, false, emptySet()), printer, false)
            }
        } finally {
            TimeZone.setDefault(timeZone)
        }
    }

    @Test
    fun dump() {
        val dumper = graphDumper(true, setOf(BigInteger::class.java)) { out, value ->
            if (value is BigInteger) out.append(value.toInt() + 1)
        }
        assertEquals("2", dumper(StringBuilder(128), BigInteger.valueOf(1)).toString())
        assertEquals("3", dumper.dump(BigInteger.valueOf(2)).toString())
        assertEquals("4", dumper.toString(BigInteger.valueOf(3)))
    }
}
