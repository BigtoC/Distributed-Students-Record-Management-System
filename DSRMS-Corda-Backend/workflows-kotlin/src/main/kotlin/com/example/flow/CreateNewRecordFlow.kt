package com.example.flow

import co.paralleluniverse.fibers.Suspendable
import com.example.contract.RecordContract.Companion.RECORD_CONTRACT_ID
import com.example.state.RecordState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.hours
import java.security.PublicKey
import java.time.LocalDateTime
import kotlin.math.sign


/**
 * This flow is for Course, Activity and Conduct
 */
object CreateNewRecordFlow {
    @InitiatingFlow
    @StartableByRPC
    class Initiator(
            private val issuer: Party,
            private val receiver: Party,
            private val eventType: CommandData,
            private val eventDescriptions: String,
            private val eventValue: String,
            private val fileReference: String
            ) : RecordBaseFlow() {

        override val progressTracker: ProgressTracker = tracker()

        companion object {
            object Initialising : ProgressTracker.Step("Performing initial steps.")
            object Building : ProgressTracker.Step("Building and verifying transaction.")
            object Signing : ProgressTracker.Step("Signing transaction.")
            object Collecting : ProgressTracker.Step("Collecting responder's signature.") {
                // Subject Team Leader Create a course, responder teachers sign
                override fun childProgressTracker() = CollectSignaturesFlow.tracker()
            }
            object Finalising : ProgressTracker.Step("Finalising transaction.") {
                override fun childProgressTracker() = FinalityFlow.tracker()
            }
            fun tracker() = ProgressTracker(Initialising, Building, Signing, Collecting, Finalising)
        }

        @Suspendable
        override fun call(): SignedTransaction {

            // Step 1 - Initial a record.
            progressTracker.currentStep = Initialising
            val receiverFlowSession = initiateFlow(receiver)
            val courseState = RecordState(
                    issuer, receiver, eventType, eventDescriptions, eventValue, fileReference, LocalDateTime.now(), null
            )
            val issuerKey = courseState.issuer.owningKey

            // Step 2 - Building
            val signerList: List<PublicKey> = courseState.participants.map { it.owningKey }
            progressTracker.currentStep = Building
            val txBuilder = TransactionBuilder(randNotary)
                    .addOutputState(courseState, RECORD_CONTRACT_ID)
                    .addCommand(eventType, signerList)
                    .setTimeWindow(serviceHub.clock.instant(), 720.hours)

            // Step 3 - Sign the transaction
            progressTracker.currentStep = Signing
            val signedTx = serviceHub.signInitialTransaction(txBuilder, issuerKey)

            // Step 4 - Get counter-parties' signatures
            progressTracker.currentStep = Collecting
            val ctx = subFlow(CollectSignaturesFlow(
                    signedTx,
                    setOf(receiverFlowSession),
                    listOf(issuerKey),
                    Collecting.childProgressTracker()
            ))

            // Step 5 - Finalise transaction
            progressTracker.currentStep = Finalising
            return subFlow(FinalityFlow(
                    ctx,
                    setOf(receiverFlowSession),
                    Finalising.childProgressTracker()
            ))

        }
    }

    @InitiatedBy(Initiator::class)
    class Responder(private val otherFlow: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherFlow, tracker()) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val output = stx.tx.outputs.single().data
                    "This must be an IOU transaction." using (output is RecordState)
                }
            }
            val stx = subFlow(SignTxFlowNoChecking(otherFlow))
            return subFlow(ReceiveFinalityFlow(otherFlow, stx.id))
        }
    }
}




