package ch.softappeal.yass.generate

import java.io.*
import java.nio.charset.*
import java.nio.file.*

abstract class Out protected constructor(path: Path) {
    private val printer: PrintWriter
    private var buffer: Appendable? = null
    private var tabs = 0

    init {
        Files.createDirectories(path.parent)
        printer = PrintWriter(path.toFile(), StandardCharsets.UTF_8.name())
    }

    protected fun redirect(buffer: Appendable?) {
        this.buffer = buffer
    }

    protected fun print(s: CharSequence) {
        if (buffer != null) buffer!!.append(s) else printer.print(s)
    }

    protected fun println() {
        print("\n")
    }

    protected fun println2() {
        println()
        println()
    }

    protected fun println(s: CharSequence) {
        print(s)
        println()
    }

    protected fun inc() {
        tabs++
    }

    protected fun dec() {
        check(tabs > 0)
        tabs--
    }

    protected fun tab() {
        print("    ")
    }

    protected fun tabs() {
        for (t in 0 until tabs) tab()
    }

    protected fun tabs(s: CharSequence) {
        tabs()
        print(s)
    }

    protected fun tabsln(s: CharSequence) {
        tabs(s)
        println()
    }

    protected fun includeFile(path: Path) {
        Files.newBufferedReader(path, StandardCharsets.UTF_8).use { input ->
            while (true) {
                val s = input.readLine() ?: break
                println(s)
            }
        }
    }

    protected fun close() {
        printer.close()
        check(!printer.checkError()) // needed because PrintWriter doesn't throw IOException
    }
}
