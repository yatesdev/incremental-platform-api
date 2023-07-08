package com.incremental.api

import com.incremental.api.formats.JacksonMessage
import com.incremental.api.formats.imageFile
import com.incremental.api.formats.jacksonMessageLens
import com.incremental.api.formats.nameField
import com.incremental.api.formats.strictFormBody
import com.incremental.api.graphql.UserDbHandler
import com.incremental.api.partner.*
import com.incremental.api.routes.ExampleContractRoute
import org.http4k.client.JavaHttpClient
import org.http4k.contract.bind
import org.http4k.contract.contract
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.OpenAPIJackson.auto
import org.http4k.contract.openapi.v3.OpenApi3
import org.http4k.contract.security.ApiKeySecurity
import org.http4k.core.*
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Status.Companion.OK
import org.http4k.filter.DebuggingFilters.PrintRequest
import org.http4k.lens.Query
import org.http4k.lens.int
import org.http4k.routing.bind
import org.http4k.routing.graphQL
import org.http4k.routing.routes
import org.http4k.security.InsecureCookieBasedOAuthPersistence
import org.http4k.security.OAuthProvider
import org.http4k.security.google
import org.http4k.server.Http4kServer
import org.http4k.server.Undertow
import org.http4k.server.asServer
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

//// Google OAuth Example
//// Browse to: http://localhost:9000/oauth - you'll be redirected to google for authentication
//val googleClientId = "myGoogleClientId"
//val googleClientSecret = "myGoogleClientSecret"
//
//// this is a test implementation of the OAuthPersistence interface, which should be
//// implemented by application developers
//val oAuthPersistence = InsecureCookieBasedOAuthPersistence("Google")
//
//// pre-defined configuration exist for common OAuth providers
//val oauthProvider = OAuthProvider.google(
//    JavaHttpClient(),
//    Credentials(googleClientId, googleClientSecret),
//    Uri.of("http://localhost:9000/oauth/callback"),
//    oAuthPersistence,
//)
//

//
//val app: HttpHandler = routes(
//    "/partners" bind partnerRoutes,
//    "/ping" bind GET to {
//        Response(OK).body("pong")
//    },
//
//    "/formats/multipart" bind POST to { request ->
//        // to extract the contents, we first extract the form and then extract the fields from it using the lenses
//        // NOTE: we are "using" the form body here because we want to close the underlying file streams
//        strictFormBody(request).use {
//            println(nameField(it))
//            println(imageFile(it))
//        }
//
//        Response(OK)
//    },
//
//    "/formats/json/jackson" bind GET to {
//        Response(OK).with(jacksonMessageLens of JacksonMessage("Barry", "Hello there!"))
//    },
//
//    "/testing/hamkrest" bind GET to { request ->
//        Response(OK).body("Echo '${request.bodyString()}'")
//    },
//
//    "/testing/kotest" bind GET to { request ->
//        Response(OK).body("Echo '${request.bodyString()}'")
//    },
//
//    "/contract/api/v1" bind contract {
//        renderer = OpenApi3(ApiInfo("IncrementalPlatformApi API", "v1.0"))
//
//        // Return Swagger API definition under /contract/api/v1/swagger.json
//        descriptionPath = "/swagger.json"
//
//        // You can use security filter tio protect routes
//        security = ApiKeySecurity(Query.int().required("api"), { it == 42 }) // Allow only requests with &api=42
//
//        // Add contract routes
//        routes += ExampleContractRoute()
//    },
//
//    "/oauth" bind routes(
//        "/" bind GET to oauthProvider.authFilter.then { Response(OK).body("hello!") },
//        "/callback" bind GET to oauthProvider.callback,
//    ),
//
//    "/graphql" bind graphQL(UserDbHandler()),
//)

fun startApp(): Http4kServer {
    val db = Database.connect(
        "jdbc:postgresql://localhost:5432/tw_platform",
        driver = "org.postgresql.Driver",
        user = "tw_platform",
        password = "tw_platform",
    )

    val partnerRepository = PartnerRepositoryImpl()
    val partnerTxManager = PartnerRepositoryTransactionManager(db, partnerRepository)

    val listPartnersHandler = ListPartnersHandlerImpl(partnerTxManager)
    val partnerRoutes = PartnerRouter(listPartnersHandler)

    val app = routes(
        "/partners" bind partnerRoutes()
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
