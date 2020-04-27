package com.example.flow

import co.paralleluniverse.fibers.Suspendable
import com.example.contract.RecordContract.Companion.RECORD_CONTRACT_ID
import com.example.state.RecordState
import com.example.contract.RecordContract.RecordCommands.*
import net.corda.core.flows.*
import net.corda.confidential.SwapIdentitiesFlow
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.hours
import net.corda.core.utilities.unwrap
import java.security.PublicKey
import java.time.LocalDateTime

object GossipRecordFlow {
    @InitiatingFlow
    @StartableByRPC
    class Initiator(
            private val receiver: Party,
            private val eventValueHash: SecureHash
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
            val anonymous = true
            receiverFlowSession.send(anonymous)
            val anonymousState = createAnonymousRecord(receiverFlowSession)
            val issuerKey = anonymousState.issuer.owningKey

            // Step 2 - Building
            val signerList: List<PublicKey> = anonymousState.participants.map { it.owningKey }
            progressTracker.currentStep = Building
            val txBuilder = TransactionBuilder(randNotary)
                    .addOutputState(anonymousState, RECORD_CONTRACT_ID)
                    .addCommand(Gossip(), signerList)
                    .setTimeWindow(serviceHub.clock.instant(), 24.hours)

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
        @Suspendable
        private fun createAnonymousRecord(receiverSession: FlowSession): RecordState {
            val anonymousIdentitiesResult = subFlow(SwapIdentitiesFlow(receiverSession))

            return RecordState(
                    anonymousIdentitiesResult[ourIdentity]!!, anonymousIdentitiesResult[receiverSession.counterparty]!!,
                    Gossip(), "Anonymous Gossip", eventValueHash.toString(), "",
                    LocalDateTime.now(), null
            )
        }
    }

    @InitiatedBy(Initiator::class)
    class Responder(private val otherFlow: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val anonymous = otherFlow.receive<Boolean>().unwrap { it }
            if (anonymous) {
                subFlow(SwapIdentitiesFlow(otherFlow))
            }

            val stx = subFlow(SignTxFlowNoChecking(otherFlow))
            return subFlow(ReceiveFinalityFlow(otherFlow, stx.id))
        }
    }
}




