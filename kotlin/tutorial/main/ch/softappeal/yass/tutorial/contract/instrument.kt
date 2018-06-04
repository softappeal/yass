package ch.softappeal.yass.tutorial.contract.instrument

import ch.softappeal.yass.remote.OneWay
import ch.softappeal.yass.tutorial.contract.Expiration
import ch.softappeal.yass.tutorial.contract.Instrument

interface InstrumentService {
    val instruments: List<Instrument>
    /** This method does nothing meaningful. It just shows how to make [OneWay] method calls. */
    @OneWay
    fun showOneWay(testBoolean: Boolean, testInt: Int)
}

class Bond(id: Int, name: String, val coupon: Double, val expiration: Expiration) : Instrument(id, name)
