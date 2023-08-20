package com.incremental.api.partner

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.incremental.api.search.*
import org.http4k.contract.openapi.OpenAPIJackson.auto
import org.http4k.core.*
import org.http4k.core.Method.GET
import org.http4k.format.read
import org.http4k.lens.Query
import org.http4k.lens.base64
import org.http4k.lens.int
import org.http4k.lens.long
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes

val offsetLens = Query.long().defaulted("offset", 0)
val limitLens = Query.int().defaulted("limit", 20)
val searchLens = Query.base64().map(jacksonObjectMapper().read<Search<Partners>>()).defaulted("search", Search())

class PartnerRouter(
    private val listPartners: ListPartnersHandler,
    private val searchPredicateFactory: SearchPredicateFactory
) {
    operator fun invoke(): RoutingHttpHandler = routes(
        "/" bind GET to listPartners()
    )

    // equivalent to controller method
    private fun listPartners(): HttpHandler = { req ->
        val offset = offsetLens(req)
        val limit = limitLens(req)

        val search = searchPredicateFactory.build(
            searchLens(req)
        )

        val result = listPartners(search, limit, offset)
        Response(Status.OK).with(Body.auto<Collection<Partner>>().toLens() of result)
    }
}

