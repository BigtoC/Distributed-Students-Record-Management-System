package com.example.test.flow

import com.example.state.RecordState
import com.example.contract.RecordContract.RecordCommands.*
import org.junit.Test
import kotlin.test.assertEquals

class CreateQuizFlowTests : RecordBaseTests() {

    @Test
    fun `Teacher and Student get the same linear id from the same course and quiz`() {

        val courseState = createRecordState(
                nodeA, nodeB, Course(), "Chinese", "Chinese", "xxx"
        )

        val linearId: String = (nodeA.services.loadState(courseState.tx.outRef<RecordState>(0).ref).data as RecordState).linearId.toString()

        val quizState = setRelatedState(
                nodeA, nodeB, Quiz(), "Chinese", "Chinese", "xxx", linearId
        )

        val teacherRefLinearId: String = (nodeA.services.loadState(quizState.tx.outRef<RecordState>(0).ref).data as RecordState).refStateId.toString()
        val studentRefLinearId: String = (nodeA.services.loadState(quizState.tx.outRef<RecordState>(0).ref).data as RecordState).refStateId.toString()

        assertEquals(linearId == teacherRefLinearId, teacherRefLinearId == studentRefLinearId)
    }

}