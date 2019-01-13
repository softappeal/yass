package ch.softappeal.yass.serialize

interface SSerializer {
    suspend fun read(reader: SReader): Any?
    suspend fun write(writer: SWriter, value: Any?)
}
