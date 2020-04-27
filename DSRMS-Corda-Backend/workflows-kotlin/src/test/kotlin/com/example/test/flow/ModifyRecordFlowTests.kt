package com.example.test.flow

import com.example.contract.RecordContract
import com.example.state.RecordState
import com.example.contract.RecordContract.RecordCommands.*
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.UniqueIdentifier
import org.junit.Test
import kotlin.test.assertEquals

class ModifyRecordFlowTests : RecordBaseTests() {

    @Test
    fun `Teacher Grade the Quiz and then modify the quiz grade`() {

        val courseState = createRecordState(
                nodeA, nodeB, Course(), "Chinese", "Chinese", "xxx"
        )
        network.waitQuiescent()

        val linearId: String = (nodeA.services.loadState(courseState.tx.outRef<RecordState>(0).ref).data as RecordState).linearId.toString()

        val quizState = setRelatedState(
                nodeA, nodeB, Quiz(), "Chinese", "Chinese", "xxx", linearId
        )
        network.waitQuiescent()

        val teacherRefLinearId: String = (nodeA.services.loadState(quizState.tx.outRef<RecordState>(0).ref).data as RecordState).refStateId.toString()
        val studentRefLinearId: String = (nodeB.services.loadState(quizState.tx.outRef<RecordState>(0).ref).data as RecordState).refStateId.toString()
        val quizLinearId: String = (nodeB.services.loadState(courseState.tx.outRef<RecordState>(0).ref).data as RecordState).linearId.toString()

        val doQuizState = setRelatedState(
                nodeB, nodeA, DoQuiz(), "This is quiz for $studentRefLinearId",
                "A,B,C", "null", quizLinearId
        )
        network.waitQuiescent()

        val studentDoQuizRefLinearId: String = (nodeB.services.loadState(
                doQuizState.tx.outRef<RecordState>(0).ref).data as RecordState).refStateId.toString()

        val gradeQuizState = setRelatedState(
                nodeA, nodeB, Grade(), "Grade for quiz $quizLinearId",
                "A", "null", studentDoQuizRefLinearId
        )

        val gradeRef: String = (nodeB.services.loadState(
                gradeQuizState.tx.outRef<RecordState>(0).ref).data as RecordState).refStateId.toString()
        val receivedEventValue: String = (nodeB.services.loadState(
                gradeQuizState.tx.outRef<RecordState>(0).ref).data as RecordState).eventValue

        val receivedEventDes: String = (nodeB.services.loadState(
                gradeQuizState.tx.outRef<RecordState>(0).ref).data as RecordState).eventDescriptions

        val valueToBeModified: String = "A+"

        val receivedFileRef: String = (nodeB.services.loadState(
                gradeQuizState.tx.outRef<RecordState>(0).ref).data as RecordState).fileReference

        val receivedRefId: String? = (nodeB.services.loadState(
                gradeQuizState.tx.outRef<RecordState>(0).ref).data as RecordState).refStateId

        val receivedLinearId: UniqueIdentifier = (nodeB.services.loadState(
                gradeQuizState.tx.outRef<RecordState>(0).ref).data as RecordState).linearId

        val modifiedState = modifyState(
                nodeA, nodeB, receivedEventDes, valueToBeModified,
                receivedFileRef, receivedRefId, receivedLinearId
        )

        val issuedModifiedValue: String = (nodeA.services.loadState(
                modifiedState.tx.outRef<RecordState>(0).ref).data as RecordState).eventValue

        val receivedModifiedValue: String = (nodeB.services.loadState(
                modifiedState.tx.outRef<RecordState>(0).ref).data as RecordState).eventValue

        nodeB.transaction {
            assertEquals(linearId, teacherRefLinearId)
            assertEquals(teacherRefLinearId, studentRefLinearId)
            assertEquals(studentDoQuizRefLinearId, quizLinearId)
            assertEquals(gradeRef, studentDoQuizRefLinearId)
            assertEquals("A", receivedEventValue)
            assertEquals("A+", receivedModifiedValue)
            assertEquals(true, receivedModifiedValue == issuedModifiedValue)
        }
    }

}