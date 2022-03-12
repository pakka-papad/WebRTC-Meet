package com.example.webrtcmeet.webrtc

import com.example.webrtcmeet.Constants.ANSWER
import com.example.webrtcmeet.Constants.ANSWER_CANDIDATE
import com.example.webrtcmeet.Constants.CALLS_COLLECTION
import com.example.webrtcmeet.Constants.CANDIDATES_COLLECTION
import com.example.webrtcmeet.Constants.END_CALL
import com.example.webrtcmeet.Constants.OFFER
import com.example.webrtcmeet.Constants.OFFER_CANDIDATE
import com.example.webrtcmeet.Constants.SDP
import com.example.webrtcmeet.Constants.SDP_CANDIDATE
import com.example.webrtcmeet.Constants.SDP_MID
import com.example.webrtcmeet.Constants.SDP_MLINEINDEX
import com.example.webrtcmeet.Constants.SERVER_URL
import com.example.webrtcmeet.Constants.TYPE
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

class SignalingClient(
    private val meetingID: String,
    private val listener: SignalingClientListener
) : CoroutineScope {
    private val db = FirebaseFirestore.getInstance()
    private var SDPtype: String? = null
    private val job = Job()
    override val coroutineContext = Dispatchers.IO + job

    init {
        connect()
    }

    private fun connect() = launch {
        db.enableNetwork().addOnSuccessListener {
            listener.onConnectionEstablished()
        }
        try {
            db.collection(CALLS_COLLECTION).document(meetingID)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) return@addSnapshotListener
                    if (snapshot != null && snapshot.exists()) {
                        val data = snapshot.data
                        if (data?.containsKey(TYPE)!! && data.getValue(TYPE).toString() == OFFER) {
                            listener.onOfferReceived(
                                SessionDescription(
                                    SessionDescription.Type.OFFER,
                                    data[SDP].toString()
                                )
                            )
                            SDPtype = OFFER
                        } else if (data.containsKey(TYPE) && data.getValue(TYPE)
                                .toString() == ANSWER
                        ) {
                            listener.onAnswerReceived(
                                SessionDescription(
                                    SessionDescription.Type.ANSWER,
                                    data[SDP].toString()
                                )
                            )
                            SDPtype = ANSWER
                        } else if (data.containsKey(TYPE) && data.getValue(TYPE)
                                .toString() == END_CALL
                        ) {
                            listener.onCallEnded()
                            SDPtype = END_CALL
                        }
                    }
                }
            db.collection(CALLS_COLLECTION).document(meetingID).collection(CANDIDATES_COLLECTION)
                .addSnapshotListener { querysnapshot, e ->
                    if (e != null) return@addSnapshotListener
                    if (querysnapshot != null && !querysnapshot.isEmpty) {
                        for (dataSnapShot in querysnapshot) {
                            val data = dataSnapShot.data
                            if (SDPtype == OFFER && data.containsKey(TYPE) && data.getValue(TYPE) == OFFER_CANDIDATE) {
                                listener.onIceCandidateReceived(
                                    IceCandidate(
                                        data[SDP_MID].toString(),
                                        Math.toIntExact(data[SDP_MLINEINDEX] as Long),
                                        data[SDP_CANDIDATE].toString()
                                    )
                                )
                            } else if (SDPtype == ANSWER && data.containsKey(TYPE) && data.getValue(
                                    TYPE
                                ) == ANSWER_CANDIDATE
                            ) {
                                listener.onIceCandidateReceived(
                                    IceCandidate(
                                        data[SDP_MID].toString(),
                                        Math.toIntExact(data[SDP_MLINEINDEX] as Long),
                                        data[SDP_CANDIDATE].toString()
                                    )
                                )
                            }

                        }
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sendIceCandidate(candidate: IceCandidate?, isJoin: Boolean) = runBlocking {
        val type = if (isJoin) ANSWER_CANDIDATE else OFFER_CANDIDATE
        val candidateConstant = mapOf(
            SERVER_URL to candidate?.serverUrl,
            SDP_MID to candidate?.sdpMid,
            SDP_MLINEINDEX to candidate?.sdpMLineIndex,
            SDP_CANDIDATE to candidate?.sdp,
            TYPE to type
        )
        db.collection(CALLS_COLLECTION).document(meetingID).collection(CANDIDATES_COLLECTION)
            .document(type).set(candidateConstant as Map<*, *>)
    }

    fun destroy() = job.complete()
}