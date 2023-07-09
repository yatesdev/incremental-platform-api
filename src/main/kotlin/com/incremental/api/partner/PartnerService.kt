package com.incremental.api.partner

import com.incremental.api.database.TransactionManager
import com.incremental.api.search.SearchPredicateFactory
import org.jetbrains.exposed.sql.Op

interface ListPartnersHandler {
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

