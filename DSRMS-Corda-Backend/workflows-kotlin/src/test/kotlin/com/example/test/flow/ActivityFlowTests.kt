package com.example.test.flow

import com.example.state.RecordState
import com.example.contract.RecordContract.RecordCommands.*
import net.corda.core.transactions.SignedTransaction
import org.junit.Test
import java.lang.NullPointerException
import kotlin.test.assertEquals

class ActivityFlowTests : RecordBaseTests() {

    @Test
    fun `Take attendance successfully`() {
        val activityState = createRecordState(
                nodeA, nodeB, Activity(), "A completion of Gaming", "Game Completion", "xxx"
        )
        val activityValue: String? = (nodeB.services.loadState(
                activityState.tx.outRef<RecordState>(0).ref).data as RecordState).eventValue

        lateinit var attendanceState: SignedTransaction
        if (activityValue != null) {
            val activityRefId: String? = (nodeB.services.loadState(
                    activityState.tx.outRef<RecordState>(0).ref).data as RecordState).refStateId

            attendanceState = setRelatedState(
                    nodeA, nodeB, Attendance(), "This is attendance for $activityRefId",
                    "2020-04-14", "null", activityRefId
            )
        }
        else {
            throw NullPointerException("No activity can take attendance")
        }

        val attendanceRecord: String = (nodeB.services.loadState(
                attendanceState.tx.outRef<RecordState>(0).ref).data as RecordState).eventValue

        nodeB.transaction {
            assertEquals("Game Completion", activityValue)
            assertEquals("2020-04-14", attendanceRecord)
        }
    }

    @Test
    fun `Issuer a certification successfully`() {
        val activityState = createRecordState(
                nodeA, nodeB, Activity(), "A completion of Gaming", "Game Completion", "xxx"
        )
        val activityValue: String? = (nodeB.services.loadState(
                activityState.tx.outRef<RecordState>(0).ref).data as RecordState).eventValue

        lateinit var certificateState: SignedTransaction
        if (activityValue != null) {
            val activityRefId: String? = (nodeB.services.loadState(
                    activityState.tx.outRef<RecordState>(0).ref).data as RecordState).refStateId

            certificateState = setRelatedState(
                    nodeA, nodeB, Certification(), "This is certificate for $activityRefId",
                    "Good", "null", activityRefId
            )
        }
        else {
            throw NullPointerException("No activity can take attendance")
        }

        nodeB.transaction {
            assertEquals("Game Completion", activityValue)

        }
    }

}