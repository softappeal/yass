package ch.softappeal.yass.tutorial.shared.web

import ch.softappeal.yass.StdErr
import ch.softappeal.yass.namedThreadFactory
import java.util.concurrent.Executors

const val Host = "0.0.0.0"
const val Port = 9090
const val WsPath = "/ws"
const val XhrPath = "/xhr"
const val WebPath = "ts"

val DispatchExecutor = Executors.newCachedThreadPool(namedThreadFactory("dispatchExecutor", StdErr))
