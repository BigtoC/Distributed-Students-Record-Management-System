package com.example.flow

import co.paralleluniverse.fibers.Suspendable
import com.example.contract.RecordContract
import com.example.state.RecordState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.hours
import java.time.LocalDateTime

object ModifyRecordFlow {
    @InitiatingFlow
    @StartableByRPC
    class Initiator (
            private val issuer: Party,
            private val receiver: Party,
            private val eventDescriptions: String,
            private val eventValue: String,
            private val fileReference: String,
            private  val refLinearId: String?,
            private val linearId: UniqueIdentifier
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

            // Step 1 - Retrieving the input from the vault.
            progressTracker.currentStep = Initialising
            val receiverFlowSession = initiateFlow(receiver)
            val inputStateAndRef  = getRecordStateByLinearId(linearId)
            val inputState = inputStateAndRef.state.data
            val eventType = inputState.eventType

            val outState = inputState.copy(
                    issuer, receiver, eventType, eventDescriptions, eventValue, fileReference, LocalDateTime.now(), refLinearId
            )
            val issuerKey = outState.issuer.owningKey

            flowCheck(inputState.issuer.nameOrNull().toString() == outState.issuer.nameOrNull().toString()) {
                "The issuers of input state and output state must be the same person"
            }

            // Step 2 - Building
            progressTracker.currentStep = Building
            val txBuilder = TransactionBuilder(randNotary)
                    .addInputState(inputStateAndRef)
                    .addOutputState(outState, RecordContract.RECORD_CONTRACT_ID)
                    .addCommand(eventType, outState.participants.map { it.owningKey })
                    .setTimeWindow(serviceHub.clock.instant(), 168.hours)

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
    class Responder(private val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(counterpartySession) {
                override fun checkTransaction(stx: SignedTransaction) {
                    val ledgerTx = stx.toLedgerTransaction(serviceHub, false)
                    val issuer = ledgerTx.inputsOfType<RecordState>().single().issuer
                    if (issuer != counterpartySession.counterparty) {
                        throw FlowException("Only the issuer can modify a record.")
                    }
                }
            }

            val stx = subFlow(signTransactionFlow)
            return subFlow(ReceiveFinalityFlow(counterpartySession, stx.id))
        }
    }

}