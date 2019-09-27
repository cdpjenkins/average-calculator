import org.http4k.client.ApacheClient
import org.http4k.core.*
import org.http4k.core.Status.Companion.OK
import org.http4k.filter.ServerFilters
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Http4kServer
import org.http4k.server.Jetty
import org.http4k.server.asServer

val app = averageCalculatorHandler()

fun main() {

    val server = makeServer()

    val jettyServer = server.start()

    val request = Request(Method.GET, "http://localhost:9001").query("name", "John Doe")

    val client = ApacheClient()

    println(client(request))

    jettyServer.stop()
}

fun makeServer(): Http4kServer {
    val server = app.asServer(Jetty(9001))
    return server
}

fun averageCalculatorHandler(): HttpHandler = ServerFilters.CatchLensFailure.then(
    routes(
        "/ping" bind Method.GET to { _: Request -> Response(OK) },
        "/say-hello" bind Method.GET to { request: Request -> Response(OK).body("Hello, ${request.query("name")}!") },
        "/calculate-mean" bind Method.POST to { request: Request -> averageShizzle() }
    )
)

private fun averageShizzle(): Response {
    return Response(OK).body("2.5")
}
