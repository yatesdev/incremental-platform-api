package com.incremental.api.database.types

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.IDateColumnType
import org.jetbrains.exposed.sql.Table
import java.time.OffsetDateTime
import java.time.ZoneOffset

fun Table.offsetDateTime(name: String): Column<OffsetDateTime> =
    registerColumn(name = name, type = PostgresOffsetDateTimeColumn())

private class PostgresOffsetDateTimeColumn : ColumnType(), IDateColumnType {
    override val hasTimePart = true

    override fun sqlType(): String = "TIMESTAMPTZ"
    override fun valueFromDB(value: Any): OffsetDateTime = when (value) {
        is OffsetDateTime -> value
        is java.sql.Timestamp -> value.toLocalDateTime().atOffset(ZoneOffset.UTC)
        is String -> OffsetDateTime.parse(value)
        else -> error("Unexpected value of type OffsetDateTime: $value of ${value::class.qualifiedName}")
    }

    override fun notNullValueToDB(value: Any): java.sql.Timestamp = when (value) {
        is OffsetDateTime -> java.sql.Timestamp.from(value.toInstant())
        is String -> java.sql.Timestamp.from(OffsetDateTime.parse(value).toInstant())
        else -> error("Unexpected value of type OffsetDateTime: $value of ${value::class.qualifiedName}")
    }
}