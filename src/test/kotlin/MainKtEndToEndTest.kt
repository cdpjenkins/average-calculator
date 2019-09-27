import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.client.ApacheClient
import org.http4k.core.*
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Status.Companion.OK
import org.http4k.filter.ServerFilters
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Http4kServer
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


object MainKtEndToEndTest {
    val server = makeServer()
    val client = ApacheClient()

    @BeforeEach
    internal fun setUp() {
        server.start()
    }

    @Test
    internal fun `says hello`() {
        // WHEN
        val result = client(Request(GET, "http://localhost:9001/say-hello").query("name", "Mr Cheese"))

        // THEN
        assertThat(result, hasStatus(OK).and(hasBody("Hello, Mr Cheese!")))
    }

    @Test
    internal fun `response to ping`() {
        // WHEN
        val result = client(Request(GET, "http://localhost:9001/ping"))

        // THEN
        assertThat(result, hasStatus(OK))
    }

    @Test
    internal fun `calculates mean`() {
        // WHEN
        val result = client(
            Request(POST, "http://localhost:9001/calculate-mean")
                .body(
                    """"
                    {
                       "numbers": [1, 2, 3, 4]
                    }
                    """.trimMargin()
                )
        )

        // THEN
        assertThat(result, hasStatus(OK))
    }

    private fun makeServer(): Http4kServer {
        val app = averageCalculatorHandler()
        val server = app.asServer(Jetty(9001))
        return server
    }

    fun averageCalculatorHandler(): HttpHandler = ServerFilters.CatchLensFailure.then(
        routes(
            "/ping" bind GET to { _: Request -> Response(OK) },
            "/say-hello" bind GET to { request: Request -> Response(OK).body("Hello, ${request.query("name")}!") },
            "/calculate-mean" bind POST to { request: Request -> Response(OK) }
        )
    )
}
