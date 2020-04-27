package com.example.flow

import com.google.common.collect.ImmutableList
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import com.example.state.RecordState
import net.corda.core.flows.*
import net.corda.core.internal.randomOrNull


/**
 * An abstract FlowLogic class that is subclassed by the record0 flows to
 * provide helper methods and classes.
 */
abstract class RecordBaseFlow : FlowLogic<SignedTransaction>() {
    val randNotary get() = randomNotary()

    fun getRecordStateByLinearId(linearId: UniqueIdentifier) : StateAndRef<RecordState> {
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(
                null,
                ImmutableList.of(linearId),
                Vault.StateStatus.UNCONSUMED,
                null
        )

        return serviceHub.vaultService.queryBy<RecordState>(queryCriteria).states.singleOrNull()
                ?: throw FlowException("Obligation with id $linearId not found.")
    }

    fun resolveIdentity(abstractParty: AbstractParty): AbstractParty {
        return serviceHub.identityService.requireWellKnownPartyFromAnonymous(abstractParty)
    }

    private fun randomNotary(): Party {
        // Todo: Future improvement - Apply Oracle to receive a large prime number
        val notaryList = serviceHub.networkMapCache.notaryIdentities
        val numOfNotary = notaryList.size

        return notaryList.randomOrNull()?: throw FlowException("No available notary.")
    }

}

internal class SignTxFlowNoChecking(otherFlow: FlowSession) : SignTransactionFlow(otherFlow) {
    override fun checkTransaction(stx: SignedTransaction) {
        // TODO: Add checking here.
    }
}

/**
 * A version of check that throws a FlowException rather than an IllegalArgumentException
 */
inline fun flowCheck(value: Boolean, msg: () -> String) {
    if (!value) {
        throw FlowException(msg())
    }
}
