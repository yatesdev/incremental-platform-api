package com.incremental.api.partner

import com.incremental.api.database.TransactionManager

interface ListPartnersHandler {
    operator fun invoke(limit: Int?, offset: Long?): Collection<Partner>
}

class ListPartnersHandlerImpl(
    private val dbConnection: TransactionManager<PartnerRepository>
) : ListPartnersHandler {
    override fun invoke(limit: Int?, offset: Long?) =
        dbConnection.tx {
            search(predicate = null, limit = limit, offset = offset)
        }
}
