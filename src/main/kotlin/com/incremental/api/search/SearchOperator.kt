package com.incremental.api.search

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize

enum class SearchFilterOperator(@JsonValue val key: String) {
    BETWEEN("between"),
    CONTAINS("contains"),
    EQUALS("eq"),
    GREATER_THAN("gt"),
    GREATER_THAN_EQUALS("gte"),
    LESS_THAN("lt"),
    LESS_THAN_EQUALS("lte"),
    IN("in"),
    NOT_EQUALS("notEquals"),
    NOT_SET("notSet"),
    SET("set"),
}

@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
@JsonSubTypes(
    value = [
        JsonSubTypes.Type(LogicalSearchOperator::class),
        JsonSubTypes.Type(FilteringSearchOperator::class)
    ]
)
sealed interface SearchOperator<T>

@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
@JsonSubTypes(
    value = [
        JsonSubTypes.Type(OrLogicalSearchOperator::class),
        JsonSubTypes.Type(AndLogicalSearchOperator::class)
    ]
)
sealed interface LogicalSearchOperator<T> : SearchOperator<T>

@JsonDeserialize(using = JsonDeserializer.None::class)
data class OrLogicalSearchOperator<T>(
    val or: List<SearchOperator<T>>
) : LogicalSearchOperator<T>

@JsonDeserialize(using = JsonDeserializer.None::class)
data class AndLogicalSearchOperator<T>(
    val and: List<SearchOperator<T>>
) : LogicalSearchOperator<T>

@JsonDeserialize(using = JsonDeserializer.None::class)
data class FilteringSearchOperator<T>(
    val member: String,
    val operator: SearchFilterOperator,
    val values: List<String>
) : SearchOperator<T>

data class Search<T>(
    val filters: Collection<SearchOperator<T>> = emptyList()
)