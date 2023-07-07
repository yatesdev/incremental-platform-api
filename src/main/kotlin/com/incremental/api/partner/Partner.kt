package com.incremental.api.partner

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.selectAll
import java.util.UUID

object Partners : UUIDTable() {
//    val id: Column<UUID> = uuid("id")
    val name: Column<String> = text("name")
//    override val primaryKey = PrimaryKey(id)
}

@JsonIgnoreProperties("table", "column")
class Partner(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Partner>(Partners)
    var name by Partners.name
}

data class PartnerResponse(
    val id: UUID,
    val name: String
)

// data class Partner(
//    val name: String
// ) {
//     companion object {
//         fun fromRow()
//     }
// }
