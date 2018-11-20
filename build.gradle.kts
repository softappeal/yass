defaultTasks("clean", "todo", "build", "dokka")

task("todo") {
    doLast {
        val fileTree = fileTree(mapOf("dir" to "."))
        fileTree
            .exclude("/.git/")
            .exclude("/.gradle/")
            .exclude("/.idea/")
            .exclude("**/out/")
            .exclude("**/build/")
        fun divider(type: Char) = println(type.toString().repeat(120))
        fun search(marker: String, help: String) {
            divider('=')
            println("= $marker: $help")
            fileTree.visit(Action {
                if (!isDirectory) {
                    var found = false
                    var number = 0
                    file.forEachLine { line ->
                        number++
                        if (line.toLowerCase().contains(marker)) {
                            if (!found) {
                                divider('-')
                                println("+ $relativePath")
                            }
                            found = true
                            println("- $number: $line")
                        }
                    }
                }
            })
        }
        search('$' + "$$", "not allowed for building a release")
        search('$' + "todo", "under construction, yet a release can still be built")
        search('$' + "note", "important comment")
        divider('=')
    }
}
