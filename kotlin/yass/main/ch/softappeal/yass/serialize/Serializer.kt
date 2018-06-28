package ch.softappeal.yass.serialize

import java.io.ObjectInputStream
import java.io.ObjectOutputStream

interface Serializer {
    @Throws(Exception::class)
    fun read(reader: Reader): Any?

    @Throws(Exception::class)
    fun write(writer: Writer, value: Any?)
}

val JavaSerializer = object : Serializer {
    override fun read(reader: Reader) =
        ObjectInputStream(reader.stream()).use(ObjectInputStream::readObject)

    override fun write(writer: Writer, value: Any?) =
        ObjectOutputStream(writer.stream()).use { it.writeObject(value) }
}
