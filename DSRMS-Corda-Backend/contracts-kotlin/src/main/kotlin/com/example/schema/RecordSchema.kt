package com.example.schema

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

/**
 * The family of schemas for IOUState.
 */
object RecordSchema

/**
 * An IOUState schema.
 */
object RecordSchemaV1 : MappedSchema(
        schemaFamily = RecordSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentRecord::class.java)) {
    @Entity
    @Table(name = "iou_states")
    class PersistentRecord(
            @Column(name = "issuer")
            var issuerName: String,

            @Column(name = "receiver")
            var borrowerName: String,

            @Column(name = "eventType")
            var eventType: String,

            @Column(name = "eventDescriptions")
            var eventDescriptions: String,

            @Column(name = "eventValue")
            var eventValue: String,

            @Column(name = "fileReference")
            var fileReference: String,

            @Column(name = "proposedTime")
            var proposedTime: LocalDateTime,

            @Column(name = "linear_id")
            var linearId: UUID
    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor(): this(
                "", "", "", "",
                "", "", LocalDateTime.now(), UUID.randomUUID()
        )
    }
}