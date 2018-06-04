package ch.softappeal.yass.tutorial.contract.instrument.stock

import ch.softappeal.yass.tutorial.contract.Instrument

/** Shows deep nesting. */
class Stock(id: Int, name: String, val paysDividend: Boolean?) : Instrument(id, name)
