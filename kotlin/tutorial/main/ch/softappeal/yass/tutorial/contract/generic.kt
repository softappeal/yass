package ch.softappeal.yass.tutorial.contract.generic

import ch.softappeal.yass.tutorial.contract.PriceKind

open class Pair<F, S>(val first: F, val second: S)

class PairBoolBool(first: Boolean, second: Boolean) : Pair<Boolean, Boolean>(first, second)

class Triple<F, T>(first: F, second: Boolean, val third: T) : Pair<F, Boolean>(first, second)

class TripleWrapper(val triple: Triple<PriceKind, Pair<String, List<PairBoolBool>>>)

interface GenericEchoService {
    fun echo(value: Pair<Boolean, TripleWrapper>): Pair<Boolean, TripleWrapper>
}
