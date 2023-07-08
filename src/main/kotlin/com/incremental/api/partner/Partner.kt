package com.incremental.api.partner

import com.incremental.api.database.TransactionManager
import com.incremental.api.database.types.offsetDateTime
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.OffsetDateTime
import java.util.UUID

object Partners : UUIDTable() {
    val name: Column<String> = text("name")
    val createdAt: Column<OffsetDateTime> = offsetDateTime("created_at")
    val updatedAt: Column<OffsetDateTime> = offsetDateTime("updated_at")
    val deletedAt: Column<OffsetDateTime> = offsetDateTime("deleted_at")
    val keyId: Column<String> = text("key_id")
    val category: Column<PartnerCategory> = enumerationByName("category", 25, PartnerCategory::class)
}

enum class PartnerCategory {
    RETAIL,
    MARKETING,
    OPERATIONS,
    FORECASTING,
    FINANCE,
    DIGITAL_SHELF
}

data class Partner(
    val id: UUID,
    val keyId: String,
    val name: String,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
    val category: PartnerCategory
)

interface PartnerRepository {
    fun findById(id: UUID): Partner?
    fun search(predicate: Any?, limit: Int?, offset: Long?): Collection<Partner>
}

class PartnerRepositoryImpl : PartnerRepository {
    override fun findById(id: UUID): Partner? =
        Partners.select { Partners.id eq id }.singleOrNull()?.toPartner()

    override fun search(predicate: Any?, limit: Int?, offset: Long?): Collection<Partner> =
        Partners.selectAll().apply {
            limit?.let {
                this.limit(limit, offset ?: 0L)
            }
        }.map { it.toPartner() }
}

class PartnerRepositoryTransactionManager(
    private val database: Database,
    private val repository: PartnerRepository
) : TransactionManager<PartnerRepository> {
    override fun <T> tx(block: PartnerRepository.() -> T) =
        transaction(database) {
            block(repository)
        }
}

fun ResultRow.toPartner() = Partner(
    id = this[Partners.id].value,
    keyId = this[Partners.keyId],
    name = this[Partners.name],
    createdAt = this[Partners.createdAt],
    updatedAt = this[Partners.updatedAt],
    category = this[Partners.category]
)

//val query = StarWarsFilms.selectAll()
//directorName?.let {
//    query.andWhere { StarWarsFilms.director eq it }
//}
//sequelId?.let {
//    query.andWhere { StarWarsFilms.sequelId eq it }
//}
//
//actorName?.let {
//    query.adjustColumnSet { innerJoin(Actors, {StarWarsFilms.sequelId}, {Actors.sequelId}) }
//        .adjustSlice { slice(fields + Actors.columns) }
//        .andWhere { Actors.name eq actorName }
//}

//@JsonIgnoreProperties("table", "column")
//class Partner(id: EntityID<UUID>) : UUIDEntity(id) {
//    companion object : UUIDEntityClass<Partner>(Partners)
//    val name by Partners.name
//    val createdAt by Partners.createdAt
//    val updatedAt by Partners.updatedAt
//    val deletedAt by Partners.deletedAt
//    val keyId by Partners.keyId
//    val category by Partners.category
//}
