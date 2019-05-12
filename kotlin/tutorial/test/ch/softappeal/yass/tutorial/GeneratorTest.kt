package ch.softappeal.yass.tutorial

import ch.softappeal.yass.tutorial.generate.*
import java.io.*
import java.util.*
import kotlin.test.*

private fun compareDirs(dir1: File, dir2: File) {
    dir1.listFiles().forEach {
        val file2 = File(dir2, it.name)
        if (!file2.exists())
            throw RuntimeException("$file2 is missing on second")
        else if (it.isDirectory && !file2.isDirectory)
            throw RuntimeException("$it: first is a dir, second is a file")
        else if (!it.isDirectory && file2.isDirectory)
            throw RuntimeException("$it: first is a file, second is a dir")
        else if (it.isDirectory)
            compareDirs(it, file2)
        else if (!Arrays.equals(it.readBytes(), file2.readBytes()))
            throw RuntimeException("$it: content is different")
    }
    dir2.listFiles().forEach {
        val file1 = File(dir1, it.name)
        if (!file1.exists()) throw RuntimeException("$it is missing on first")
    }
}

private fun compareGenerated(module: String) {
    compareDirs(File("../../$module/tutorial/generated"), File("build/generated/$module"))
}

class GeneratorTest {
    @Test
    fun generateTypeScript() {
        GenerateTypeScript.main()
        compareGenerated("ts")
    }

    @Test
    fun generatePython3() {
        GeneratePython3.main()
        compareGenerated("py3")
    }
}
