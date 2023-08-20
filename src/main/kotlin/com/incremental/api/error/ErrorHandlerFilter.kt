package com.incremental.api.error

import org.http4k.contract.openapi.OpenAPIJackson.auto
import org.http4k.core.Body
import org.http4k.core.Filter
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with

open class HttpException(val status: Status, message: String = status.description) : RuntimeException(message)
data class GenericErrorModelBody(val body: List<String>)
data class GenericErrorModel(val errors: GenericErrorModelBody)

object CatchHttpExceptions {
//    private val logger = LoggerFactory.getLogger(CatchHttpExceptions::class.java)

    operator fun invoke() = Filter { next ->
        {
            try {
                next(it)
            } catch (e: HttpException) {
//                logger.error("Uncaught error: ", e)
                createErrorResponse(e.status, listOf(e.message ?: "Oops!"))
            } catch (e: Throwable) {
//                logger.error("Uncaught error: ", e)
                createErrorResponse(Status(422, "Unprocessable Entity"), listOf("Unexpected error"))
            }
        }
    }
}

private val error = Body.auto<GenericErrorModel>().toLens()

fun createErrorResponse(status: Status, errorMessages: List<String>) =
    Response(status).with(error of GenericErrorModel(GenericErrorModelBody(errorMessages)))