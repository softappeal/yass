task("clean") {
    doLast {
        ant.withGroovyBuilder {
            "delete"("dir" to "build")
            "delete"("dir" to "node_modules")
            "delete"("dir" to ".", "includes" to "**/*.js", "excludes" to "test/test-node.js")
            "delete"("dir" to ".", "includes" to "**/*.js.map")
        }
    }
}
