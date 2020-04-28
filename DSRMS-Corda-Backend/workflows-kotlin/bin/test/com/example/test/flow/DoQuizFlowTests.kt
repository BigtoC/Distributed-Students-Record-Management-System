package com.example.test.flow

import com.example.state.RecordState
import com.example.contract.RecordContract.RecordCommands.*
import org.junit.Test
import kotlin.test.assertEquals

class DoQuizFlowTests : RecordBaseTests() {

    @Test
    fun `Student do the quiz with the same linear id from the same course and quiz`() {

        val courseState = createRecordState(
                nodeA, nodeB, Course(), "Chinese", "Chinese", "xxx"
        )
        network.waitQuiescent()

        val linearId: String = (nodeA.services.loadState(courseState.tx.outRef<RecordState>(0).ref).data as RecordState).linearId.toString()

        val quizState = setRelatedState(
                nodeA, nodeB, Quiz(), "Chinese", "Chinese", "xxx", linearId
        )

        val teacherRefLinearId: String = (nodeA.services.loadState(quizState.tx.outRef<RecordState>(0).ref).data as RecordState).refStateId.toString()
        val studentRefLinearId: String = (nodeB.services.loadState(quizState.tx.outRef<RecordState>(0).ref).data as RecordState).refStateId.toString()
        val quizLinearId: String = (nodeB.services.loadState(courseState.tx.outRef<RecordState>(0).ref).data as RecordState).linearId.toString()

        val doQuizState = setRelatedState(
                nodeB, nodeA, DoQuiz(), "This is quiz for $studentRefLinearId",
                "A,B,C", "null", quizLinearId
        )

        val studentDoQuizRefLinearId: String = (nodeB.services.loadState(
                doQuizState.tx.outRef<RecordState>(0).ref).data as RecordState).refStateId.toString()


        nodeB.transaction {
            assertEquals(linearId, teacherRefLinearId)
            assertEquals(teacherRefLinearId, studentRefLinearId)
            assertEquals(studentDoQuizRefLinearId, quizLinearId)
        }
    }

}