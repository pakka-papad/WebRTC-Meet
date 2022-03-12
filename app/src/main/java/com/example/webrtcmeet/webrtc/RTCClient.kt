package com.example.webrtcmeet.webrtc

import android.app.Application
import com.example.webrtcmeet.Constants.ANSWER_CANDIDATE
import com.example.webrtcmeet.Constants.CALLS_COLLECTION
import com.example.webrtcmeet.Constants.CANDIDATES_COLLECTION
import com.example.webrtcmeet.Constants.END_CALL
import com.example.webrtcmeet.Constants.OFFER_CANDIDATE
import com.example.webrtcmeet.Constants.SDP
import com.example.webrtcmeet.Constants.SDP_MID
import com.example.webrtcmeet.Constants.SDP_MLINEINDEX
import com.example.webrtcmeet.Constants.TYPE
import com.google.firebase.firestore.FirebaseFirestore
import org.webrtc.*

class RTCClient(
    context: Application,
    observer: PeerConnection.Observer
) {
    private val LOCAL_TRACK_ID = "local_track"
    private val LOCAL_STREAM_ID = "local_track"
    private val rootEglBase = EglBase.create()

    private val iceServer = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer()
    )
    private var localAudioTrack: AudioTrack? = null
    private var localVideoTrack: VideoTrack? = null
    private val peerConnectionFactory by lazy {
        PeerConnectionFactory.builder()
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(rootEglBase.eglBaseContext))
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(rootEglBase.eglBaseContext, true, true))
            .setOptions(
                PeerConnectionFactory.Options().apply {
                    disableEncryption = true
                    disableEncryption = true
                }
            )
            .createPeerConnectionFactory()
    }
    private val videoCapturer by lazy {
        Camera2Enumerator(context).run {
            deviceNames.find {
                isFrontFacing(it)
            }?.let {
                createCapturer(it, null)
            } ?: throw IllegalStateException()
        }
    }
    private val localAudioSource by lazy { peerConnectionFactory.createAudioSource(MediaConstraints()) }
    private val localVideoSource by lazy { peerConnectionFactory.createVideoSource(false) }
    private val peerConnection by lazy { peerConnectionFactory.createPeerConnection(iceServer, observer) }
    private var remoteSessionDescription: SessionDescription? = null
    val db = FirebaseFirestore.getInstance()

    init {
        initPeerConnectionFactory(context)
    }

    private fun initPeerConnectionFactory(context: Application) {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)
    }

    fun initSurfaceView(view: SurfaceViewRenderer) = view.run {
        setMirror(true)
        setEnableHardwareScaler(true)
        init(rootEglBase.eglBaseContext, null)
    }

    fun startLocalVideoCapture(localVideoOutput: SurfaceViewRenderer) {
        val surfaceTextureHelper =
            SurfaceTextureHelper.create(Thread.currentThread().name, rootEglBase.eglBaseContext)
        (videoCapturer as VideoCapturer).initialize(
            surfaceTextureHelper,
            localVideoOutput.context,
            localVideoSource.capturerObserver
        )
        videoCapturer.startCapture(320, 240, 60)
        localAudioTrack =
            peerConnectionFactory.createAudioTrack(LOCAL_TRACK_ID + "_audio", localAudioSource)
        localVideoTrack = peerConnectionFactory.createVideoTrack(LOCAL_TRACK_ID, localVideoSource)
        localVideoTrack?.addSink(localVideoOutput)
        val localStream = peerConnectionFactory.createLocalMediaStream(LOCAL_STREAM_ID)
        localStream.addTrack(localAudioTrack)
        localStream.addTrack(localVideoTrack)
        peerConnection?.addStream(localStream)
    }

    private fun PeerConnection.call(sdpObserver: SdpObserver, meetingID: String) {
        val constraints = MediaConstraints().apply {
            mandatory.add(
                MediaConstraints.KeyValuePair(
                    "OfferToReceiveVideo",
                    "true"
                )
            )
        }
        createOffer(object : SdpObserver by sdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {
                setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}

                    override fun onSetSuccess() {
                        val offer = mapOf(
                            SDP to p0?.description,
                            TYPE to p0?.type
                        )
                        db.collection(CALLS_COLLECTION).document(meetingID).set(offer)
                    }

                    override fun onCreateFailure(p0: String?) {}

                    override fun onSetFailure(p0: String?) {}

                }, p0)
                sdpObserver.onCreateSuccess(p0)
            }

            override fun onSetSuccess() {}

            override fun onCreateFailure(p0: String?) {}

            override fun onSetFailure(p0: String?) {}
        }, constraints)
    }

    private fun PeerConnection.answer(sdpObserver: SdpObserver, meetingID: String) {
        val constraints = MediaConstraints().apply {
            mandatory.add(
                MediaConstraints.KeyValuePair(
                    "OfferToReceiveVideo",
                    "true"
                )
            )
        }
        createAnswer(object : SdpObserver by sdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {
                val answer = mapOf(
                    SDP to p0?.description,
                    TYPE to p0?.type
                )
                db.collection(CALLS_COLLECTION).document(meetingID).set(answer)
                setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}

                    override fun onSetSuccess() {}

                    override fun onCreateFailure(p0: String?) {}

                    override fun onSetFailure(p0: String?) {}
                }, p0)
                sdpObserver.onCreateSuccess(p0)
            }

            override fun onCreateFailure(p0: String?) {
            }
        }, constraints)
    }

    fun call(sdpObserver: SdpObserver, meetingID: String) =
        peerConnection?.call(sdpObserver, meetingID)

    fun answer(sdpObserver: SdpObserver, meetingID: String) =
        peerConnection?.answer(sdpObserver, meetingID)

    fun onRemoteSessionReceived(sessionDescription: SessionDescription) {
        remoteSessionDescription = sessionDescription
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}

            override fun onSetSuccess() {}

            override fun onCreateFailure(p0: String?) {}

            override fun onSetFailure(p0: String?) {}
        }, sessionDescription)
    }

    fun addIceCandidate(iceCandidate: IceCandidate?) {
        peerConnection?.addIceCandidate(iceCandidate)
    }

    fun endCall(meetingID: String) {
        db.collection(CALLS_COLLECTION).document(meetingID).collection(CANDIDATES_COLLECTION).get()
            .addOnSuccessListener {
                val iceCandidateArray = mutableListOf<IceCandidate>()
                for (dataSnapShot in it) {
                    if (dataSnapShot.contains(TYPE) && dataSnapShot[TYPE] == OFFER_CANDIDATE) {
                        iceCandidateArray.add(
                            IceCandidate(
                                dataSnapShot[SDP_MID].toString(),
                                Math.toIntExact(dataSnapShot[SDP_MLINEINDEX] as Long),
                                dataSnapShot[SDP].toString()
                            )
                        )
                    } else if (dataSnapShot.contains(TYPE) && dataSnapShot[TYPE] == ANSWER_CANDIDATE) {
                        iceCandidateArray.add(
                            IceCandidate(
                                dataSnapShot[SDP_MID].toString(),
                                Math.toIntExact(dataSnapShot[SDP_MLINEINDEX] as Long),
                                dataSnapShot[SDP].toString()
                            )
                        )
                    }
                }
                peerConnection?.removeIceCandidates(iceCandidateArray.toTypedArray())
            }
        val endCall = mapOf(TYPE to END_CALL)
        db.collection(CALLS_COLLECTION).document(meetingID)
            .set(endCall)
        peerConnection?.close()
    }
}