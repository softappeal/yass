task("clean") {
    doLast {
        ant.withGroovyBuilder {
            "delete"("dir" to ".", "includes" to "**/__pycache__/", "includeemptydirs" to true)
            "delete"("dir" to ".mypy_cache")
        }
    }
}
