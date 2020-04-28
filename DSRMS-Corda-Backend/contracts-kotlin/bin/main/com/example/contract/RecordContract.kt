package com.example.contract

import com.example.state.RecordState
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.transactions.LedgerTransaction
import java.security.PublicKey

/**
 * This contract enforces rules regarding the creation of a valid [RecordContract],
 * which in turn encapsulates an [RecordContract].
 * All contracts must sub-class the [Contract] interface.
 */
class RecordContract : Contract {
    companion object {
        const val RECORD_CONTRACT_ID = "com.example.contract.RecordContract"
    }

    /**
     * Different command types (also eventType for a state)
     */
    interface RecordCommands : CommandData {
        class Course : TypeOnlyCommandData(), RecordCommands  //Teachers create a course for students
        class Quiz : TypeOnlyCommandData(), RecordCommands  //Teachers create a quiz related to a course for students
        class DoQuiz : TypeOnlyCommandData(), RecordCommands  //Students do the quiz
        class Grade : TypeOnlyCommandData(), RecordCommands  //Teachers grade student for the quiz
        class Activity : TypeOnlyCommandData(), RecordCommands  //Extracurricular activities
        class Attendance : TypeOnlyCommandData(), RecordCommands  //Attendances for the extracurricular activities
        class Certification : TypeOnlyCommandData(), RecordCommands  //Certifications for the extracurricular activities
        class Conduct : TypeOnlyCommandData(), RecordCommands  //操行
        class Gossip: TypeOnlyCommandData(), RecordCommands  // Anonymous record
        class Modify: TypeOnlyCommandData(), RecordCommands  // To state that this Tx is for modification
    }

    /**
     * The verify() function of all the states' contracts must not throw an exception for a transaction are valid.
     */
    override fun verify(tx: LedgerTransaction) {
        requireThat {
            //"Shape" Constraint, fit for all situation
            "Only accept one command" using(tx.commands.size == 1)
            "Output must be a RecordState" using(tx.outputStates[0] is RecordState)
        }

        val command = tx.commands.requireSingleCommand<RecordCommands>()

        when (command.value) {
            is RecordCommands.Course -> verifyCourse(tx)
            is RecordCommands.Quiz -> verifyQuiz(tx)
            is RecordCommands.DoQuiz -> verifyDoQuiz(tx)
            is RecordCommands.Grade -> verifyGrade(tx)
            is RecordCommands.Activity -> verifyActivity(tx)
            is RecordCommands.Attendance -> verifyAttendance(tx)
            is RecordCommands.Certification -> verifyCertification(tx)
            is RecordCommands.Conduct -> verifyConduct(tx)
            is RecordCommands.Gossip -> verifyAnonymous(tx)
            else -> throw IllegalArgumentException("Unrecognised command.")
        }
    }

    /**
     * Checklist:
     * 1. "Shape" Constraint - number of input/output states, number of commands
     * 2. Content Constraint - business(业务) Constraint
     * 3. Required Signer Constraint - 需要哪些人签名
     */
    private fun basicConstraintVerifications(
            output: RecordState,
            command: CommandWithParties<RecordCommands>
    ) = requireThat {
        val issuer: AbstractParty = output.issuer
        val receiver: AbstractParty = output.receiver
        val issuerKey: PublicKey = issuer.owningKey
        val receiverKey: PublicKey = receiver.owningKey

        "Issuer must sign" using(command.signers.contains(issuerKey))
        "Receiver must sign" using(command.signers.contains(receiverKey))
        "Issuer and receiver must not be the same person" using(output.issuer != output.receiver)
    }

    private fun signerConstraintForCourse(
            output: RecordState
    ) = requireThat {
        val issuer: AbstractParty = output.issuer
        val receiver: AbstractParty = output.receiver

        "Signer must be a teacher" using(issuer.nameOrNull()?.organisationUnit == "Teacher")
        "Receiver must be a student" using(receiver.nameOrNull()?.organisationUnit == "Student")
        "Issuer and receiver must be in the same school" using(issuer.nameOrNull()?.locality == receiver.nameOrNull()?.locality)
        "Issuer and receiver must not be the same person" using(output.issuer != output.receiver)
    }

    private fun verifyCourse(tx: LedgerTransaction) = requireThat {
//        "Create a course must have no input" using(tx.inputStates.isEmpty())
        "Create a course must have only one output" using(tx.outputStates.size == 1)

        "OutputState must be a RecordState" using(tx.outputStates[0] is RecordState)
        val output = tx.outputsOfType<RecordState>()[0]

        val command = tx.commands.requireSingleCommand<RecordCommands>()
        basicConstraintVerifications(output, command)
        signerConstraintForCourse(output)

    }

    private fun verifyQuiz(tx: LedgerTransaction) = requireThat {
//        "Create a quiz must have no input" using(tx.inputStates.isEmpty())
        "Create a quiz must have only one output" using(tx.outputStates.size == 1)
        val output = tx.outputsOfType<RecordState>()[0]

        val command = tx.commands.requireSingleCommand<RecordCommands>()
        basicConstraintVerifications(output, command)
        signerConstraintForCourse(output)
    }

    private fun verifyDoQuiz(tx: LedgerTransaction) = requireThat {
//        "Do a quiz must have no input" using(tx.inputStates.isEmpty())
        "Do a quiz must have only one output" using(tx.outputStates.size == 1)
        val output = tx.outputsOfType<RecordState>()[0]

        val command = tx.commands.requireSingleCommand<RecordCommands>()
        basicConstraintVerifications(output, command)
        val issuer: AbstractParty = output.issuer
        val receiver: AbstractParty = output.receiver
        "Issuer and receiver must be in the same school" using(issuer.nameOrNull()?.locality == receiver.nameOrNull()?.locality)
        "Issuer and receiver must not be the same person" using(output.issuer != output.receiver)
    }

    private fun verifyGrade(tx: LedgerTransaction) = requireThat {
//        "Grade a quiz must have no input" using(tx.inputStates.isEmpty())
        "Grade a quiz must have only one output" using(tx.outputStates.size == 1)
        val output = tx.outputsOfType<RecordState>()[0]

        val grade = output.eventValue
        "Grading must not be empty" using(grade.isNotBlank() && grade.isNotEmpty())

        val command = tx.commands.requireSingleCommand<RecordCommands>()
        basicConstraintVerifications(output, command)
        signerConstraintForCourse(output)
    }

    private fun verifyActivity(tx: LedgerTransaction) = requireThat {
//        "Create an activity must have no input" using(tx.inputStates.isEmpty())
        "Create an event must have one output" using(tx.outputStates.size == 1)
        val output = tx.outputsOfType<RecordState>()[0]
        val command = tx.commands.requireSingleCommand<RecordCommands>()

        basicConstraintVerifications(output, command)
    }

    private fun verifyAttendance(tx: LedgerTransaction) = requireThat {
//        "Take attendance must have no input" using(tx.inputStates.isEmpty())
        "Take attendance must have one output" using(tx.outputStates.size == 1)
        val output = tx.outputsOfType<RecordState>()[0]
        val command = tx.commands.requireSingleCommand<RecordCommands>()

        basicConstraintVerifications(output, command)
    }

    private fun verifyCertification(tx: LedgerTransaction) = requireThat {
//        "Issue a certification must have no input" using(tx.inputStates.isEmpty())
        "Issue a certification must have one output" using(tx.outputStates.size == 1)
        val output = tx.outputsOfType<RecordState>()[0]
        val command = tx.commands.requireSingleCommand<RecordCommands>()

        val fileRef = output.fileReference
        "Issue a certification must have a file" using(fileRef.isNotEmpty() && fileRef.isNotBlank())

        basicConstraintVerifications(output, command)
    }

    private fun verifyConduct(tx: LedgerTransaction) = requireThat {
//        "Issue conduct must have no input" using(tx.inputStates.isEmpty())
        "Issue conduct must have one output" using(tx.outputStates.size == 1)
        val output = tx.outputsOfType<RecordState>()[0]
        val command = tx.commands.requireSingleCommand<RecordCommands>()

//        "Issue conduct must have only one signer" using(command.signers.size == 1)

        val issuer: AbstractParty = output.issuer
        val issuerKey: PublicKey = issuer.owningKey
        "Issuer must sign" using(command.signers.contains(issuerKey))
        "Signer must be a teacher" using(issuer.nameOrNull()?.organisationUnit == "Teacher")
    }

    private fun verifyAnonymous(tx: LedgerTransaction) = requireThat {
        "Issue conduct must have one output" using(tx.outputStates.size == 1)
        val output = tx.outputsOfType<RecordState>()[0]
        val command = tx.commands.requireSingleCommand<RecordCommands>()

        val des: String = output.eventValue
//        println("EventDescription: $des")

//        "Description must include anonymous" using(des.contains("Anonymous"))

        val issuer: AbstractParty = output.issuer
        val issuerKey: PublicKey = issuer.owningKey
        "Issuer must sign" using(command.signers.contains(issuerKey))

    }
}
