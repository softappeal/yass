package ch.softappeal.yass.tutorial.contract.generic;

public interface GenericEchoService {

    Pair<Boolean, TripleWrapper> echo(Pair<Boolean, TripleWrapper> value);

}
