package com.incremental.api.partner

import com.incremental.api.database.TransactionManager
import com.incremental.api.error.HttpException
import org.http4k.core.Status
import org.jetbrains.exposed.sql.Op
import java.util.UUID
import kotlin.reflect.KClass

fun interface ListPartnersHandler {
    operator fun invoke(search: Op<Boolean>, limit: Int?, offset: Long?): Collection<Partner>
}

class ListPartnersHandlerImpl(
    private val dbConnection: TransactionManager<PartnerRepository>,
) : ListPartnersHandler {
    override fun invoke(search: Op<Boolean>, limit: Int?, offset: Long?) =
        dbConnection.tx {
            search(predicate = search, limit = limit, offset = offset)
        }
}

fun interface ReadPartnerHandler {
    operator fun invoke(id: UUID): Partner
}

class ReadPartnerHandlerImpl(
    private val dbConnection: TransactionManager<PartnerRepository>,
) : ReadPartnerHandler {
    override fun invoke(id: UUID): Partner =
        dbConnection.tx {
            findById(id) ?: throw PartnerNotFoundException("ID '$id' does not exist.")
        }

}

open class ResourceNotFoundException(type: KClass<*>, message : () -> String) : HttpException(status = Status.NOT_FOUND, message().let { "${type.simpleName} not found: $it"})
class PartnerNotFoundException(message: String) : ResourceNotFoundException(Partner::class, {message})