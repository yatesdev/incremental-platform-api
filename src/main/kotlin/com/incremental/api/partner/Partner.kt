package com.incremental.api.partner

import com.incremental.api.database.TransactionManager
import com.incremental.api.database.types.offsetDateTime
import com.incremental.api.partner.PartnerEntity.Companion.referrersOn
import com.incremental.api.search.FilteringSearchOperator
import com.incremental.api.search.SearchFilterOperator
import com.incremental.api.search.SearchFilterPredicateBuilder
import com.incremental.api.search.findColumn
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.OffsetDateTime
import java.util.*
import kotlin.reflect.KClass

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
    DIGITAL_SHELF;

    object PartnerCategoryExpressionBuilder : SearchFilterPredicateBuilder(PartnerCategory::class) {
        private fun exposedOrmStrategy(filter: FilteringSearchOperator<*>, entity: Table): Op<Boolean> =
            with(filter) {
                val path = entity.findColumn<PartnerCategory>(filter.member)
                    ?: throw IllegalArgumentException(
                        "Path of ${filter.member} does not exist on ${entity::class.simpleName}"
                    )

                val partnerCategories = values.map { valueOf(it) }

                return when (operator) {
                    SearchFilterOperator.EQUALS -> Op.build { path eq partnerCategories.first() }
                    SearchFilterOperator.NOT_EQUALS -> Op.build { path neq partnerCategories.first() }
                    SearchFilterOperator.IN -> Op.build { path inList partnerCategories }
                    else -> throw IllegalArgumentException(
                        "${this::class.simpleName} does not support operation of '$operator'"
                    )
                }
            }

        override fun build(filter: FilteringSearchOperator<*>, entity: KClass<*>): Op<Boolean> =
            when (entity.objectInstance) {
                is Table -> exposedOrmStrategy(filter, entity.objectInstance as Table)
                else -> throw IllegalArgumentException("No strategy found for type '${entity::class.simpleName}'")
            }
    }
}

data class Partner(
    val id: UUID,
    val keyId: String,
    val name: String,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
    val category: PartnerCategory,
    val attributes: Collection<String> = emptyList()
) {
    companion object {
        fun fromEntity(entity: PartnerEntity) = with(entity) {
            Partner(
                id = id.value,
                keyId = keyId,
                name = name,
                createdAt = createdAt,
                updatedAt = updatedAt,
                category = category,
                attributes = attributes.map { it.attribute }
            )
        }
    }
}

class PartnerEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<PartnerEntity>(Partners)
    val keyId by Partners.keyId
    val name by Partners.name
    val createdAt by Partners.createdAt
    val updatedAt by Partners.updatedAt
    val category by Partners.category
    val attributes by PartnerAttribute referrersOn PartnerAttributes.partner
}

fun PartnerEntity.toPartner() = Partner.fromEntity(this)


interface PartnerRepository {
    fun findById(id: UUID): Partner?
    fun search(predicate: Op<Boolean>?, limit: Int?, offset: Long?): Collection<Partner>
}

class PartnerRepositoryImpl : PartnerRepository {
    override fun findById(id: UUID): Partner? =
        Partners.select { Partners.id eq id }.singleOrNull()?.toPartner()

    override fun search(predicate: Op<Boolean>?, limit: Int?, offset: Long?): Collection<Partner> =
        PartnerEntity.find { predicate ?: Op.nullOp() }.apply {
            limit?.let {
                this.limit(limit, offset ?: 0L)
            }
        }.map { it.toPartner() }
//        Partners.selectAll().apply {
//            predicate?.let {
//                adjustWhere { predicate }
//            }
//            limit?.let {
//                this.limit(limit, offset ?: 0L)
//            }
//        }.map { it.toPartner() }
}

class PartnerRepositoryTransactionManager(
    private val database: Database,
    private val repository: PartnerRepository
) : TransactionManager<PartnerRepository> {
    override fun <T> tx(block: PartnerRepository.() -> T) =
        transaction(database) {
            addLogger(StdOutSqlLogger)
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
