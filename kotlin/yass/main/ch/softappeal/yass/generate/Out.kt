package ch.softappeal.yass.generate

import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.io.PrintWriter
import java.nio.charset.StandardCharsets

abstract class Out protected constructor(file: String) {

    private val printer: PrintWriter
    private var buffer: Appendable? = null
    private var tabs = 0

    init {
        val directory = File(file).parentFile
        check(directory.exists() || directory.mkdirs()) { "directory '$directory' not created" }
        printer = PrintWriter(file, StandardCharsets.UTF_8.name())
    }

    protected fun redirect(buffer: Appendable?) {
        this.buffer = buffer
    }

    protected fun print(s: CharSequence) {
        if (buffer != null)
            buffer!!.append(s)
        else
            printer.print(s)
    }

    protected fun printf(format: String, vararg args: Any) {
        print(String.format(format, *args))
    }

    protected fun println() {
        printf("\n")
    }

    protected fun println2() {
        println()
        println()
    }

    protected fun printfln(format: String, vararg args: Any) {
        printf(format, *args)
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
        printf("    ")
    }

    protected fun tabs() {
        for (t in 0 until tabs) tab()
    }

    protected fun tabsf(format: String, vararg args: Any) {
        tabs()
        printf(format, *args)
    }

    protected fun tabs(s: CharSequence) {
        tabs()
        print(s)
    }

    protected fun tabsfln(format: String, vararg args: Any) {
        tabsf(format, *args)
        println()
    }

    protected fun tabsln(s: CharSequence) {
        tabs(s)
        println()
    }

    protected fun includeFile(file: String) {
        BufferedReader(InputStreamReader(FileInputStream(file), StandardCharsets.UTF_8)).use { input ->
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
