package com.example.test.flow

import com.example.contract.RecordContract.RecordCommands.*
import com.example.state.RecordState
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import com.example.flow.RecordBaseFlow
import net.corda.core.contracts.hash
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

class CreateRecordFlowTests : RecordBaseTests() {

    @Test
    fun `Create a course successfully`() {
        val courseState = createRecordState(
                nodeA, nodeB, Course(), "My Course", "Chinese", "xxx"
        )

        network.waitQuiescent()

        val recordA = nodeA.services.loadState(courseState.tx.outRef<RecordState>(0).ref).data as RecordState
        val recordB = nodeB.services.loadState(courseState.tx.outRef<RecordState>(0).ref).data as RecordState

        assertEquals(recordA, recordB)
    }

    @Test
    fun `Create a course only to B successfully`() {
        val courseState = createRecordState(
                nodeA, nodeB, Course(), "Your Course", "Math", "no")

        network.waitQuiescent()

        val recordA = nodeA.services.loadState(courseState.tx.outRef<RecordState>(0).ref).data as RecordState

        val recordC: RecordState?
        recordC = try {
            nodeC.services.loadState(courseState.tx.outRef<RecordState>(0).ref).data as RecordState
        }
        catch (e: net.corda.core.contracts.TransactionResolutionException) {
            null
        }

        assertNotEquals(recordA, recordC)
    }

    @Test
    fun `Create a conduct record successfully`() {
        val conductState = createRecordState(
                nodeA, nodeB, Conduct(), "2019-2020 student Bob's conduct", "Good", "xxx"
        )
        network.waitQuiescent()

        val receivedConduct: String = (nodeB.services.loadState(
                conductState.tx.outRef<RecordState>(0).ref).data as RecordState).eventValue
        assertEquals("Good", receivedConduct)
    }

    @Test
    fun `Create a new activity successfully`() {
        val activityState = createRecordState(
                nodeA, nodeB, Activity(), "A completion of Gaming", "Game Completion", "xxx"
        )
        network.waitQuiescent()

        val activityValue: String = (nodeB.services.loadState(
                activityState.tx.outRef<RecordState>(0).ref).data as RecordState).eventValue
        assertEquals("Game Completion", activityValue)
    }

    @Test
    fun `Create a course and send gossip successfully`() {
        val courseState = createRecordState(
                nodeA, nodeB, Course(), "My Course", "Chinese", "xxx"
        )

        network.waitQuiescent()

        val recordA = nodeA.services.loadState(courseState.tx.outRef<RecordState>(0).ref).data as RecordState
        val recordB = nodeB.services.loadState(courseState.tx.outRef<RecordState>(0).ref).data as RecordState

        val gossipState = gossipState(nodeA, nodeB, recordA.hash())

        assertEquals(recordA, recordB)
    }


}