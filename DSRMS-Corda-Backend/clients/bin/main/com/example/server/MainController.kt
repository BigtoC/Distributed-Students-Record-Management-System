package com.example.server

import com.example.contract.RecordContract.RecordCommands.*
import com.example.flow.CreateNewRecordFlow
import com.example.flow.GossipRecordFlow
import com.example.flow.ModifyRecordFlow
import com.example.flow.SetRelatedRecordFlow
import com.example.state.RecordState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.UniqueIdentifier.Companion.fromString
import net.corda.core.contracts.hash
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.getOrThrow
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.MediaType.TEXT_PLAIN_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.util.StringUtils
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import javax.servlet.http.HttpServletRequest

val SERVICE_NAMES = listOf("Notary", "Network Map Service")
val CREATE_CMD = listOf(Course(), Activity(), Conduct())
val SET_CMD = listOf(Quiz(), DoQuiz(), Grade(), Attendance(), Certification())

/**
 *  A Spring Boot Server API controller for interacting with the node via RPC.
 */

@RestController
@RequestMapping("/DSRMS/api/") // The paths for GET and POST requests are relative to this base path.
class MainController(rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val myLegalName = rpc.proxy.nodeInfo().legalIdentities.first().name
    private val proxy = rpc.proxy
    private val issuer = proxy.nodeInfo().legalIdentities.first()

    /**
     * Returns the node's name.
     */
    @GetMapping(value = [ "me" ], produces = [ APPLICATION_JSON_VALUE ])
    fun whoAmI() = mapOf("me" to myLegalName)

    /**
     * Returns all participants registered with the network map service. (from the same school)
     * These names can be used to look up identities using the identity service.
     */
    @GetMapping(value = [ "peers" ], produces = [ APPLICATION_JSON_VALUE ])
    fun getPeers(): Map<String, List<CordaX500Name>> {
        val nodeInfo = proxy.networkMapSnapshot()
        return mapOf("peers" to nodeInfo
                .map { it.legalIdentities.first().name }
                //filter out myself, notary and eventual network map started by driver
                .filter { it.locality in (SERVICE_NAMES + myLegalName.locality) })
    }

    /**
     * Returns all participants registered with the network map service.
     * These names can be used to look up identities using the identity service.
     */
    @GetMapping(value = [ "allPeople" ], produces = [ APPLICATION_JSON_VALUE ])
    fun getAllPeople(): Map<String, List<CordaX500Name>> {
        val nodeInfo = proxy.networkMapSnapshot()
        return mapOf("allPeople" to nodeInfo
                .map { it.legalIdentities.first().name }
                //filter out myself, notary and eventual network map started by driver
                .filter { it.organisation !in SERVICE_NAMES }
        )
    }

    /**
     * Initiates a flow to create record between two parties.
     */
    @PostMapping(
            value = [ "create-record" ],
            produces = [ TEXT_PLAIN_VALUE ],
            headers = [ "Content-Type=application/x-www-form-urlencoded" ]
    )
    fun createRecord(request: HttpServletRequest): ResponseEntity<String> {
        val partyName = request.getParameter("partyName")
                ?: return ResponseEntity.badRequest().body("Query parameter 'partyName' must not be null.\n")
        lateinit var x500Name: String
        getAllPeople()["allPeople"]?.forEach {
            if (it.organisation == partyName) {
                x500Name = it.toString()
            }
        }
        val partyX500Name = CordaX500Name.parse(x500Name)
        val otherParty = proxy.wellKnownPartyFromX500Name(partyX500Name)
                ?: return ResponseEntity.badRequest().body("Party named $partyName cannot be found.\n")

        val eventTypeCmd = getEventTypeCmd(request.getParameter("eventType"))

        val eventDescription: String = request.getParameter("eventDescription")
        val eventValue: String = request.getParameter("eventValue")
        val fileReference: String = request.getParameter("fileReference")

        lateinit var signedTx: SignedTransaction

        return try {
            when (eventTypeCmd) {
                in CREATE_CMD -> {
                    signedTx = proxy.startTrackedFlowDynamic(CreateNewRecordFlow.Initiator::class.java,
                            issuer, otherParty, eventTypeCmd, eventDescription, eventValue, fileReference
                    ).returnValue.getOrThrow()
                }
                in SET_CMD -> {
                    val refLinearId: String = request.getParameter("refLinearId")
                    signedTx = proxy.startTrackedFlowDynamic(SetRelatedRecordFlow.Initiator::class.java,
                            issuer, otherParty, eventTypeCmd, eventDescription, eventValue, fileReference, refLinearId
                    ).returnValue.getOrThrow()
                }
                is Gossip -> {

                }
                is Modify -> {
                    val refLinearId: String = request.getParameter("refLinearId")
                    val linearIdStr: String = request.getParameter("linearId")
                    val linearId: UniqueIdentifier = fromString(linearIdStr)
                    signedTx = proxy.startTrackedFlowDynamic(ModifyRecordFlow.Initiator::class.java,
                            issuer, otherParty, eventDescription, eventValue, fileReference, refLinearId, linearId
                    ).returnValue.getOrThrow()
                }
                else -> {
                    return ResponseEntity.badRequest().body("Wrong Event Type\n")
                }
            }

//            gossip(signedTx)

            ResponseEntity.status(HttpStatus.CREATED).body("Transaction id ${signedTx.id} committed to ledger.\n")
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }

    /**
     * Displays all records states that only this node has been involved in.
     */
    @GetMapping(value = [ "my-records" ], produces = [ APPLICATION_JSON_VALUE ])
    fun getMyRecords(): Map<String, List<StateAndRef<RecordState>>>  {
        val myRecords = proxy.vaultQueryBy<RecordState>().states.filter {
            it.state.data.issuer == proxy.nodeInfo().legalIdentities.first() ||
                    it.state.data.receiver == proxy.nodeInfo().legalIdentities.first()
        }

        return mapOf(
                "Course" to myRecords.map { it } .filter { it.state.data.eventType == Course() },
                "Quiz" to myRecords.map { it } .filter { it.state.data.eventType == Quiz() },
                "DoQuiz" to myRecords.map { it } .filter { it.state.data.eventType == DoQuiz() },
                "Grade" to myRecords.map { it } .filter { it.state.data.eventType == Grade() },
                "Activity" to myRecords.map { it } .filter { it.state.data.eventType == Activity() },
                "Attendance" to myRecords.map { it } .filter { it.state.data.eventType == Attendance() },
                "Certification" to myRecords.map { it } .filter { it.state.data.eventType == Certification() },
                "Conduct" to myRecords.map { it } .filter { it.state.data.eventType == Conduct() }
        )
    }

    @PostMapping("uploads")
    @ResponseBody
    fun uploadFiles(@RequestParam("file") file : Array<MultipartFile>): String{
        logger.info("Upload file(s)")
        val rootPath : Path = Paths.get("/files/upload")
        if (file.isEmpty()) {
            return "upload file array is empty"
        }
        try {
            for (f in file) {
                val fileName  = StringUtils.cleanPath(f.originalFilename!!)
                Files.copy(f.inputStream, rootPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING)
            }
        } catch (e : Exception) {
            logger.error("error", e)
            return "upload error"
        }
        return "success"
    }

    private fun gossip(signedTx: SignedTransaction) {
        val nodeInfo = proxy.networkMapSnapshot()
        val allNodes = nodeInfo
                .map { it }
                .filter { it.legalIdentities.first().name.organisation !in (SERVICE_NAMES + myLegalName.organisation) }

        lateinit var gossipReceivers: MutableList<Party>
        val randValue = (0..10).shuffled()[(0..10).shuffled()[2]]
        var count = 0

        allNodes.forEach {
            if (count < randValue) {
                gossipReceivers.add(it.legalIdentities[0])
                count ++
            }
        }

        val gossipRandReceivers = gossipReceivers.shuffled().take((0..gossipReceivers.size).shuffled()[0])

        gossipRandReceivers.forEach {
            val gossipTx = proxy.startTrackedFlowDynamic(
                    GossipRecordFlow.Initiator::class.java, it, signedTx.tx.outputs[0].data.hash()
            ).returnValue.getOrThrow()

        }
    }

    private fun getEventTypeCmd(eventTypeStr: String): CommandData {
        return when (eventTypeStr) {
            "Course" -> Course()
            "Quiz" -> Quiz()
            "DoQuiz" -> DoQuiz()
            "Grade" -> Grade()
            "Activity" -> Activity()
            "Attendance" -> Attendance()
            "Certification" -> Certification()
            "Conduct" -> Conduct()
            "Anonymous" -> Gossip()
            "Modify" -> Modify()
            else -> throw IllegalArgumentException("Wrong event type")
        }
    }

}
