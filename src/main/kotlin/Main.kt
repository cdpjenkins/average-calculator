import org.http4k.client.ApacheClient
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.server.Http4kServer
import org.http4k.server.Jetty
import org.http4k.server.asServer

fun main() {

    val server = makeServer()

    val jettyServer = server.start()

    val request = Request(Method.GET, "http://localhost:9001").query("name", "John Doe")

    val client = ApacheClient()

    println(client(request))

    jettyServer.stop()
}

fun makeServer(): Http4kServer {
    val app = { request: Request -> Response(OK).body("Hello, ${request.query("name")}!") }

    val server = app.asServer(Jetty(9001))
    return server
}
