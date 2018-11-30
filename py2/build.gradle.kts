task("clean") {
    doLast {
        ant.withGroovyBuilder {
            "delete"("dir" to ".", "includes" to "**/*.pyc")
            "delete"("dir" to ".mypy_cache")
        }
    }
}
