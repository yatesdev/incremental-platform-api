package com.incremental.api.partner

import com.incremental.api.partner.PartnerEntity.Companion.referrersOn
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import java.util.UUID

data class PartnerAttributePK(val partner: UUID, val attribute: String) : Comparable<PartnerAttributePK> {
    override fun compareTo(other: PartnerAttributePK): Int =
        partner.compareTo(other.partner) + attribute.compareTo(other.attribute)
}

object PartnerAttributes :  IntIdTable("partner_attributes") {
    val partner = reference("partner", Partners).uniqueIndex("unique_attr")
    val attribute = varchar("attribute", 50).uniqueIndex("unique_attr")

//    override val primaryKey = PrimaryKey(arrayOf(partner, attribute))
}

class PartnerAttribute(id: EntityID<Int>): IntEntity(id) {
    companion object : IntEntityClass<PartnerAttribute>(PartnerAttributes)
    val partner by PartnerEntity referencedOn PartnerAttributes.partner
    val attribute by PartnerAttributes.attribute
}WIP