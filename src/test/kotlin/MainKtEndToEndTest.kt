import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.client.ApacheClient
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
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
        val averageRequest: String = AverageRequest(numbers = listOf(1.0, 2.0, 3.0, 4.0)).toJsonString()
        assertThat(client( Request(POST, "http://localhost:9001/calculate-mean").body(averageRequest)), hasStatus(OK).and(hasBody("2.5")))
    }
}

object AverageCalculatorTest {
    @Test
    internal fun `average of single number is that number`() {
        app(Request(POST, "/calculate-mean").body(AverageRequest(listOf(1.0)).toJsonString()
        )).answerShouldBe("1.0")
    }

    fun Response.answerShouldBe(expected: String) {
        assertThat(this, hasStatus(OK).and(hasBody(expected)))
    }
}