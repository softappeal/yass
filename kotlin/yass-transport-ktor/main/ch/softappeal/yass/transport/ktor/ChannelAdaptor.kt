package ch.softappeal.yass.transport.ktor

import ch.softappeal.yass.serialize.*
import kotlinx.coroutines.io.*

fun ByteWriteChannel.writer() = object : SWriter() {
    override suspend fun writeByte(value: Byte) = this@writer.writeByte(value)
    override suspend fun writeBytes(buffer: ByteArray, offset: Int, length: Int) = writeFully(buffer, offset, length)
}

fun ByteReadChannel.reader() = object : SReader() {
    override suspend fun readByte(): Byte = this@reader.readByte()
    override suspend fun readBytes(buffer: ByteArray, offset: Int, length: Int) = readFully(buffer, offset, length)
}
