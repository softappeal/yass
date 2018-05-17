package ch.softappeal.yass.serialize.contractj;

import ch.softappeal.yass.Tag;

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
