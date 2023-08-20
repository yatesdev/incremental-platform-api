package com.incremental.api

import com.incremental.api.error.CatchHttpExceptions
import com.incremental.api.error.createErrorResponse
import com.incremental.api.partner.ListPartnersHandlerImpl
import com.incremental.api.partner.PartnerCategory
import com.incremental.api.partner.PartnerRepositoryImpl
import com.incremental.api.partner.PartnerRepositoryTransactionManager
import com.incremental.api.partner.PartnerRouter
import com.incremental.api.partner.ReadPartnerHandler
import com.incremental.api.partner.ReadPartnerHandlerImpl
import com.incremental.api.search.SearchFilterPredicateBuilderFactory
import com.incremental.api.search.SearchPredicateFactory
import com.incremental.api.search.StringExpressionBuilder
import org.http4k.core.HttpHandler
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.filter.DebuggingFilters.PrintRequest
import org.http4k.filter.ServerFilters
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Http4kServer
import org.http4k.server.Undertow
import org.http4k.server.asServer
import org.jetbrains.exposed.sql.Database

fun startApp(): Http4kServer {
    val db = Database.connect(
        "jdbc:postgresql://localhost:5432/tw_platform",
        driver = "org.postgresql.Driver",
        user = "tw_platform",
        password = "tw_platform",
    )

    val searchFilterPredicateBuilderFactory = SearchFilterPredicateBuilderFactory(
        setOf(StringExpressionBuilder, PartnerCategory.PartnerCategoryExpressionBuilder)
    )

    val searchPredicateFactory = SearchPredicateFactory(searchFilterPredicateBuilderFactory)

    val partnerRepository = PartnerRepositoryImpl()
    val partnerTxManager = PartnerRepositoryTransactionManager(db, partnerRepository)

    val listPartnersHandler = ListPartnersHandlerImpl(partnerTxManager)
    val readPartnerHandler = ReadPartnerHandlerImpl(partnerTxManager)
    val partnerRoutes = PartnerRouter(listPartnersHandler, readPartnerHandler, searchPredicateFactory)

    val app = CatchHttpExceptions()
        .then(ServerFilters.CatchLensFailure { error ->
            createErrorResponse(
                Status.BAD_REQUEST,
                if (error.cause != null) listOf(error.cause?.message!!) else error.failures.map { it.toString() }
            )
        })
        .then(
            routes(
                "/partners" bind partnerRoutes()
            )
        )

    val printingApp: HttpHandler = PrintRequest().then(app)
    println("Starting server...")
    val server = printingApp.asServer(Undertow(9000)).start()
    println("Server started on " + server.port())
    return server
}

fun main() {
    val server = startApp()
    server.block()
}
