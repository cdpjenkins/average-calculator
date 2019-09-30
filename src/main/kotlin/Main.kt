import org.http4k.core.*
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.filter.ServerFilters
import org.http4k.format.Jackson.auto
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Http4kServer
import org.http4k.server.Jetty
import org.http4k.server.asServer

val app = averageCalculatorHandler()

fun main() {
    val server = makeServer()
    server.start()
}

fun makeServer(): Http4kServer {
    val server = app.asServer(Jetty(9001))
    return server
}

fun averageCalculatorHandler(): HttpHandler = ServerFilters.CatchLensFailure.then(
    routes(
        "/ping" bind Method.GET to { Response(OK) },
        "/say-hello" bind Method.GET to { request: Request -> Response(OK).body("Hello, ${request.query("name")}!") },
        "/calculate-mean" bind Method.POST to MeanCalculator::handleRequest,
        "/calculate-mode" bind Method.POST to ModeCalculator::handleRequest
    )
)

data class AverageRequest(val numbers: List<Double>)

abstract class AverageCalculator {
    val averageRequestLens = Body.auto<AverageRequest>().toLens()

    internal abstract fun calculateAverage(averageRequest: AverageRequest): Double

    fun handleRequest(request: Request): Response {
        try {
            val averageRequest: AverageRequest = parseAndValidate(request)
            val result = calculateAverage(averageRequest)
            return Response(OK).body(result.toString())
        } catch (e: java.lang.IllegalArgumentException) {
            return Response(BAD_REQUEST);
        }
    }

    fun parseAndValidate(request: Request): AverageRequest {
        val averageRequest: AverageRequest = averageRequestLens(request)

        if (averageRequest.numbers.isEmpty()) {
            throw IllegalArgumentException("Missing parameter: numbers")
        }
        return averageRequest
    }
}

object MeanCalculator : AverageCalculator() {
    override fun calculateAverage(averageRequest: AverageRequest): Double {
        val mean: Double = averageRequest.numbers.reduceRight { l, r -> l + r } / averageRequest.numbers.size
        return mean
    }
}

object ModeCalculator : AverageCalculator() {
    override fun calculateAverage(averageRequest: AverageRequest): Double {
        val mode = averageRequest.numbers.groupingBy { it }.eachCount().entries.maxBy { it.value }?.key ?: -1.0
        return mode
    }
}
