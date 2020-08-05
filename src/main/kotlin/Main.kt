import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.http4k.core.*
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.filter.MetricFilters
import org.http4k.filter.ServerFilters
import org.http4k.format.Jackson.auto
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Http4kServer
import org.http4k.server.Jetty
import org.http4k.server.asServer
import java.lang.Math.abs
import java.util.*

val app = averageCalculatorHandler()
var micrometerMetricsegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

fun main() {
    val server = makeServer()
    server.start()
}

fun makeServer(): Http4kServer {
    val appWithMetrics =
        MetricFilters.Server.RequestCounter(micrometerMetricsegistry)
            .then(MetricFilters.Server.RequestTimer(micrometerMetricsegistry))
            .then(app)

    val server: Http4kServer = appWithMetrics.asServer(Jetty(9001))

    return server
}

fun averageCalculatorHandler(): HttpHandler = ServerFilters.CatchLensFailure.then(
    routes(
        "/ping" bind GET to { Response(OK) },
        "/randomDelay" bind GET to {
            randomSleep()
            Response(OK)
        },
        "/say-hello" bind GET to { request: Request -> Response(OK).body("Hello, ${request.query("name")}!") },
        "/calculate-mean" bind POST to MeanCalculator::handleRequest,
        "/calculate-mode" bind POST to ModeCalculator::handleRequest,
        "metrics"  bind GET to {request: Request -> Response(OK).body(micrometerMetricsegistry.scrape())}
    )
)

private fun randomSleep() {
    val positiveDelay = abs(Random().nextGaussian() * 500)
    Thread.sleep(positiveDelay.toLong())
}

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
