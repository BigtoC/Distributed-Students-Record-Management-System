package com.example.test.flow

import com.example.flow.*
import com.example.state.RecordState
import net.corda.core.concurrent.CordaFuture
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.CordaX500Name
import net.corda.core.transactions.SignedTransaction
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.internal.chooseIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before

abstract class RecordBaseTests {

    private val nameA = CordaX500Name("Alice Han", "Teacher", "Alice", "Pui Kiu Middle School", null, "CN")
    private val nameB = CordaX500Name("Bob Chan", "Student", "Bob", "Pui Kiu Middle School", null, "CN")
    private val nameC = CordaX500Name("Cindy Lu", "Student", "Cindy", "Pui Kiu Middle School", null, "CN")

    lateinit var network: MockNetwork
    lateinit var nodeA: StartedMockNode
    lateinit var nodeB: StartedMockNode
    lateinit var nodeC: StartedMockNode

    @Before
    fun setup() {
        network = MockNetwork(MockNetworkParameters(
                cordappsForAllNodes = listOf(
                        TestCordapp.findCordapp("com.example.schema"),
                        TestCordapp.findCordapp("com.example.state")
                ),
                threadPerNode = false,
                networkParameters = (testNetworkParameters(minimumPlatformVersion = 4))
        ))

        nodeA = network.createPartyNode(nameA)
        nodeB = network.createPartyNode(nameB)
        nodeC = network.createPartyNode(nameC)
        val nodes = listOf(nodeA, nodeB, nodeC)

        nodes.forEach {
            it.registerInitiatedFlow(CreateNewRecordFlow.Responder::class.java)
            it.registerInitiatedFlow(SetRelatedRecordFlow.Responder::class.java)
            it.registerInitiatedFlow(ModifyRecordFlow.Responder::class.java)
            it.registerInitiatedFlow(GossipRecordFlow.Responder::class.java)
        }

    }

    @After
    fun tearDown() {
        network.stopNodes()
        System.setProperty("net.corda.node.dbtransactionsresolver.InMemoryResolutionLimit", "0")
    }

    protected fun createRecordState(issuer: StartedMockNode,
                                    receiver: StartedMockNode,
                                    eventType: CommandData,
                                    eventDescriptions: String,
                                    eventValue: String,
                                    fileReference: String
    ) : SignedTransaction {
        val issuerIdentity = issuer.info.chooseIdentity()
        val receiverIdentity = receiver.info.chooseIdentity()
        val flow = CreateNewRecordFlow.Initiator(
                issuerIdentity, receiverIdentity, eventType, eventDescriptions, eventValue, fileReference
        )

        val future: CordaFuture<SignedTransaction> = issuer.startFlow(flow)

        network.runNetwork()

        return future.get()
    }

    protected fun setRelatedState(issuer: StartedMockNode,
                                  receiver: StartedMockNode,
                                  eventType: CommandData,
                                  eventDescriptions: String,
                                  eventValue: String,
                                  fileReference: String,
                                  refLinearId: String?
    ) : SignedTransaction {
        val issuerIdentity = issuer.info.chooseIdentity()
        val receiverIdentity = receiver.info.chooseIdentity()
        val flow = SetRelatedRecordFlow.Initiator(
                issuerIdentity, receiverIdentity, eventType, eventDescriptions, eventValue, fileReference, refLinearId
        )

        val future: CordaFuture<SignedTransaction> = issuer.startFlow(flow)

        network.runNetwork()

        return future.get()
    }

    protected fun modifyState(issuer: StartedMockNode,
                              receiver: StartedMockNode,
                              eventDescriptions: String,
                              eventValue: String,
                              fileReference: String,
                              refLinearId: String?,
                              linearId: UniqueIdentifier
    ) : SignedTransaction {
        val issuerIdentity = issuer.info.chooseIdentity()
        val receiverIdentity = receiver.info.chooseIdentity()
        val flow = ModifyRecordFlow.Initiator(
                issuerIdentity, receiverIdentity, eventDescriptions,
                eventValue, fileReference, refLinearId, linearId
        )

        val future: CordaFuture<SignedTransaction> = issuer.startFlow(flow)

        network.runNetwork()

        return future.get()
    }

    protected fun gossipState(
            issuer: StartedMockNode,
            receiver: StartedMockNode,
            eventValueHash: SecureHash
    ) : SignedTransaction {
        val receiverIdentity = receiver.info.chooseIdentity()
        val flow = GossipRecordFlow.Initiator(receiverIdentity, eventValueHash)

        val future: CordaFuture<SignedTransaction> = issuer.startFlow(flow)

        network.runNetwork()

        return future.get()
    }

}