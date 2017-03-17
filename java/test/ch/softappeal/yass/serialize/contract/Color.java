package ch.softappeal.yass.serialize.contract;

import ch.softappeal.yass.util.Tag;

import java.util.Objects;

@Tag(30) public enum Color {

    RED("red"),
    GREEN("green"),
    BLUE("blue");

    Color(final String text) {
        this.text = Objects.requireNonNull(text);
    }

    private final String text;

    public String getText() {
        return text;
    }

}
