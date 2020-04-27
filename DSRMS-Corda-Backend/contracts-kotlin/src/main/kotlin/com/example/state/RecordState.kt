package com.example.state

import com.example.contract.RecordContract
import com.example.schema.RecordSchemaV1
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import java.time.LocalDateTime


@BelongsToContract(RecordContract::class)
data class RecordState(
        val issuer: AbstractParty,
        val receiver: AbstractParty,
        val eventType: CommandData,
        val eventDescriptions: String,
        val eventValue: String,
        val fileReference: String,
        val proposedTime: LocalDateTime,
        val refStateId: String?,
        override val linearId: UniqueIdentifier = UniqueIdentifier()
) : LinearState, QueryableState {

    override val participants: List<AbstractParty> get() = listOf(issuer, receiver)

    val refLinearId: UniqueIdentifier get() = linearId

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is RecordSchemaV1 -> RecordSchemaV1.PersistentRecord(
                    this.issuer.nameOrNull()?.commonName.toString(),
                    this.receiver.nameOrNull()?.commonName.toString(),
                    this.eventType.toString(),
                    this.eventDescriptions,
                    this.eventValue,
                    this.fileReference,
                    this.proposedTime,
                    this.linearId.id
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(RecordSchemaV1)

    override fun toString() : String {
        val issuerString = (issuer as? Party)?.name?.organisationUnit ?: issuer.owningKey
        val receiverString = (receiver as? Party)?.name?.organisationUnit ?: issuer.owningKey
        val typeString = (eventType as? CommandData)?.toString()
        val valueString = (eventValue as? String)?.toString()
        val timeString = (proposedTime as? LocalDateTime)?.toString()
        return "Event($linearId): $issuerString issued $typeString to $receiverString with $valueString at $timeString."
    }


}
