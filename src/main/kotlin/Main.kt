import org.http4k.core.*
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.filter.ServerFilters
import org.http4k.format.Jackson.asJsonObject
import org.http4k.format.Jackson.auto
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Http4kServer
import org.http4k.server.Jetty
import org.http4k.server.asServer

import kotlin.collections.eachCount


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
        "/calculate-mean" bind Method.POST to ::calculateMean,
        "/calculate-mode" bind Method.POST to ::calculateMode
    )
)

data class AverageRequest(val numbers: List<Double>)

fun Any.toJsonString() = this.asJsonObject().toString()

val averageRequestLense = Body.auto<AverageRequest>().toLens()

private fun calculateMean(request: Request): Response {
    val averageRequest: AverageRequest = averageRequestLense(request)

    if (averageRequest.numbers.isEmpty()) {
        return Response(BAD_REQUEST);
    }

    val mean: Double = averageRequest.numbers
        .reduceRight { l, r -> l + r } / averageRequest.numbers.size

    return Response(OK).body(mean.toString())
}

fun calculateMode(request: Request): Response {
    val averageRequest: AverageRequest = averageRequestLense(request)

    val count: Map<Double, Int> = averageRequest.numbers.groupingBy{it}.eachCount()

    return Response(OK).body(count.entries.maxBy { it.value }?.key.toString())
}
