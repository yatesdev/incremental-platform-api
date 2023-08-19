package com.incremental.api.search

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.*
import java.util.UUID
import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.reflect.jvm.jvmErasure


abstract class SearchFilterPredicateBuilder(
    val type: KClass<out Any>
) {
    //    abstract fun build(filter: SearchFilter<*>, entityPath: PathBuilder<Any?>): Op<Any>
    abstract fun build(filter: FilteringSearchOperator<*>, entity: KClass<*>): Op<Boolean>
}

inline fun <reified T : Any> Table.findColumn(name: String): Column<T>? =
    this.columns.firstOrNull { it.name == name } as Column<T>?

object StringExpressionBuilder : SearchFilterPredicateBuilder(String::class) {
    private fun exposedOrmStrategy(filter: FilteringSearchOperator<*>, entity: Table): Op<Boolean> =
        with(filter) {
//            val columns = entity.columns
//            println(columns)
            val path = entity.findColumn<String>(filter.member)
                ?: throw IllegalArgumentException(
                    "Path of ${filter.member} does not exist on ${entity::class.simpleName}"
                )

            return when (operator) {
                SearchFilterOperator.EQUALS -> Op.build { path eq values.first() }
                SearchFilterOperator.NOT_EQUALS -> Op.build { path neq values.first() }
                SearchFilterOperator.IN -> Op.build { path inList values }
                SearchFilterOperator.GREATER_THAN -> Op.build { path greater values.first() }
                SearchFilterOperator.GREATER_THAN_EQUALS -> Op.build { path greaterEq values.first() }
                SearchFilterOperator.LESS_THAN -> Op.build { path less values.first() }
                SearchFilterOperator.LESS_THAN_EQUALS -> Op.build { path lessEq values.first() }
                SearchFilterOperator.CONTAINS -> Op.build { path like "%${values.first()}%" }
                SearchFilterOperator.SET -> Op.build { path.isNotNull() }
                SearchFilterOperator.NOT_SET -> Op.build { path.isNull() }
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

class SearchFilterPredicateBuilderFactory(
    handlers: Set<SearchFilterPredicateBuilder>
) {
//    private val implicitTypes = handlers.map {
//        object : SearchFilterPredicateBuilder(buildOrmColumnType(it.type)) {
//            private fun exposedOrmStrategy(filter: FilteringSearchOperator<*>, entity: KClass<*>): Op<Boolean> =
//                with(filter) {
//                    val table = entity.objectInstance!! as Table
//                    val columns = table.columns
//                    println(columns)
//                    val path = table.findColumn<String>(filter.member)
//                        ?: throw IllegalArgumentException(
//                            "Path of ${filter.member} does not exist on ${table::class.simpleName}"
//                        )
//
//                    return when (operator) {
//                        SearchFilterOperator.EQUALS -> Op.build { path eq values.first() }
//                        SearchFilterOperator.NOT_EQUALS -> Op.build { path neq values.first() }
//                        SearchFilterOperator.IN -> Op.build { path inList values }
//                        SearchFilterOperator.GREATER_THAN -> Op.build { path greater values.first() }
//                        SearchFilterOperator.GREATER_THAN_EQUALS -> Op.build { path greaterEq values.first() }
//                        SearchFilterOperator.LESS_THAN -> Op.build { path less values.first() }
//                        SearchFilterOperator.LESS_THAN_EQUALS -> Op.build { path lessEq values.first() }
//                        SearchFilterOperator.CONTAINS -> Op.build { path like "%${values.first()}%" }
//                        SearchFilterOperator.SET -> Op.build { path.isNotNull() }
//                        SearchFilterOperator.NOT_SET -> Op.build { path.isNull() }
//                        else -> throw IllegalArgumentException(
//                            "${this::class.simpleName} does not support operation of '$operator'"
//                        )
//                    }
//                }
//
//            override fun build(filter: FilteringSearchOperator<*>, entity: KClass<*>): Op<Boolean> =
//                when {
//                    entity.allSupertypes.contains(typeOf<Table>()) -> exposedOrmStrategy(filter, entity)
//                    else -> throw IllegalArgumentException("No strategy found for type '${entity.simpleName}'")
//                }
//        }
//    }
    private val handlersByType: MutableMap<KClass<*>, SearchFilterPredicateBuilder> =
        (handlers).associateBy { it.type }.toMutableMap()


    private fun buildOrmColumnType(type: KClass<*>): KClass<*> {
        val handlerKType: KType = type.createType()
        val kClass: KClass<Column<*>> = Column::class
        val kType: KType = kClass.createType(listOf(KTypeProjection(KVariance.INVARIANT, handlerKType)))
        return kType.jvmErasure
    }

    fun getHandler(type: KClass<*>): SearchFilterPredicateBuilder =
        handlersByType[type]
            ?: throw IllegalArgumentException("Handler for $type not registered.")
}

class SearchPredicateFactory(
    private val searchFilterPredicateBuilderFactory: SearchFilterPredicateBuilderFactory
) {
    private fun buildExpression(
        filter: FilteringSearchOperator<*>,
        entityPathClass: KClass<*>
    ): Op<Boolean> {
        var path: Any
        var currentPathClass: KClass<*> = entityPathClass
        val memberIterator = filter.member.split(".").iterator()
        while (memberIterator.hasNext()) {
            val memberName = memberIterator.next()
            println(currentPathClass.memberProperties)
            val currentMember = currentPathClass.memberProperties.firstOrNull { it.name == memberName }
                ?: throw IllegalArgumentException("No member '$memberName' found in ${currentPathClass.simpleName}")

            val isCollection = currentMember.returnType.isSubtypeOf(Collection::class.starProjectedType)

            if (isCollection) {
                currentMember.returnType.arguments.first().type?.jvmErasure?.let {
//                    // Special case for checking if we are filtering for an empty/nonempty collection
//                    if (filter.member.endsWith("exists")) {
//                        return if (filter.values.first() == "true") {
//                            path.getCollection(member, it.java).isNotEmpty
//                        } else {
//                            path.getCollection(member, it.java).isEmpty
//                        }
//                    } else {
//                        // Compiler can't figure out that PathBuilder<out Any!> can be safely cast to PathBuilder<Any?>
//                        // there is probably a way to eliminate the cast, but I'm not seeing it
//                        path = path.getCollection(activeMemberName, it.java).any() as PathBuilder<Any?>
//                        currPathClass = it
//                    }
                }
            } else {
                if (memberIterator.hasNext())
                    path = currentMember
//                currentPathClass = currentMember.returnType.jvmErasure
                currentPathClass = currentMember.returnType.arguments.first().type!!.jvmErasure
            }
        }

        val handler = searchFilterPredicateBuilderFactory.getHandler(currentPathClass)
        return handler.build(filter, entityPathClass)
    }


    fun <T : Any> buildPredicateFromFilters(
        filters: Collection<SearchOperator<T>>,
        entityPathClass: KClass<T>,
        entityVariable: String = entityPathClass.simpleName?.replaceFirstChar { it.lowercaseChar() }
            ?: throw IllegalArgumentException("Entity variable must be defined for this class"),
        parent: LogicalSearchOperator<T>? = null
    ): Op<Boolean> {
        var result = Op.nullOp<Boolean>()

        fun appendFilteringSearchOperator(
            searchOperator: FilteringSearchOperator<*>,
            entityClass: KClass<*>,
            parent: LogicalSearchOperator<*>?
        ) = when (parent) {
            is OrLogicalSearchOperator -> Op.build {
                result.or {
                    buildExpression(searchOperator, entityClass)
                }
            }

            is AndLogicalSearchOperator, null -> Op.build {
                result.and {
                    buildExpression(
                        searchOperator,
                        entityClass
                    )
                }
            }
        }

        fun <T : Any> appendLogicalSearchOperator(
            searchOperator: LogicalSearchOperator<T>,
            entityPathClass: KClass<T>
        ) = when (searchOperator) {
            is OrLogicalSearchOperator -> buildPredicateFromFilters(
                searchOperator.or,
                entityPathClass,
                entityVariable,
                searchOperator
            )

            is AndLogicalSearchOperator -> buildPredicateFromFilters(
                searchOperator.and,
                entityPathClass,
                entityVariable,
                searchOperator
            )
        }

//            if (parent is OrLogicalSearchOperator) {
//            when (searchOperator) {
//                is OrLogicalSearchOperator -> searchOperator.or
//                is AndLogicalSearchOperator -> searchOperator.and
//            }.let {
//                result.or {
//                    buildPredicateFromFilters(
//                        it,
//                        entityPathClass,
//                        entityVariable
//                    )
//                }
//            }
//        } else {
//            when (searchOperator) {
//                is OrLogicalSearchOperator -> searchOperator.or
//                is AndLogicalSearchOperator -> searchOperator.and
//            }.let {
//                result.and {
//                    buildPredicateFromFilters(
//                        it,
//                        entityPathClass,
//                        entityVariable
//                    )
//                }
//            }
//        }

        filters.forEach { searchOperator ->
            result = when (searchOperator) {
                is FilteringSearchOperator -> appendFilteringSearchOperator(
                    searchOperator,
                    entityPathClass,
                    parent
                )

                is LogicalSearchOperator -> appendLogicalSearchOperator(searchOperator, entityPathClass)
            }
        }
        return result
    }

    final inline fun <reified T : Any> build(
        search: Collection<SearchOperator<T>> = listOf(),
        /** Name of the static EntityPathBase defined for QueryDsl generated QType entities. (QType.qType)
         * The default value handles the common case.
         * Will need to be overridden if the entity name is a reserved word. Ex. group becomes group1 during generation
         */
        entityVariable: String = T::class.simpleName?.replaceFirstChar { it.lowercaseChar() }
            ?: throw IllegalArgumentException("Entity variable must be defined for this class")

    ) = buildPredicateFromFilters(search, T::class, entityVariable)
}