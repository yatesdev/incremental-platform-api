package com.incremental.api.partner

import org.http4k.contract.openapi.OpenAPIJackson.auto
import org.http4k.core.*
import org.http4k.core.Method.GET
import org.http4k.lens.Query
import org.http4k.lens.int
import org.http4k.lens.long
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes

val offsetLens = Query.long().defaulted("offset", 0)
val limitLens = Query.int().defaulted("limit", 20)

class PartnerRouter(
    val listPartners: ListPartnersHandler
) {
    operator fun invoke(): RoutingHttpHandler = routes(
        "/" bind GET to listPartners()
    )

    // equivalent to controller method
    private fun listPartners(): HttpHandler = { req ->
        val offset = offsetLens(req)
        val limit = limitLens(req)

        val result = listPartners(limit, offset)
        Response(Status.OK).with(Body.auto<Collection<Partner>>().toLens() of result)
    }
}

