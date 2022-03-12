package com.example.webrtcmeet

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.example.webrtcmeet.Constants.DISPLAY_NAME
import com.example.webrtcmeet.Constants.IS_JOIN
import com.example.webrtcmeet.Constants.MEETING_ID
import com.example.webrtcmeet.Constants.PHOTO_URL
import com.example.webrtcmeet.Constants.RETURN_FROM_CALL
import com.example.webrtcmeet.Constants.UID
import com.example.webrtcmeet.webrtc.*
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer

class CallActivity : AppCompatActivity() {
    private lateinit var meetingID: String
    private lateinit var photoUrl: String
    private lateinit var uid: String
    private lateinit var displayName: String
    private var isJoin: Boolean = false
    private lateinit var rtcClient: RTCClient
    private lateinit var signalingClient: SignalingClient
    private lateinit var remoteView: SurfaceViewRenderer
    private lateinit var endCallBtn: ImageButton
    private lateinit var localView: SurfaceViewRenderer
    private val sdpObserver = object: AppSdpObserver(){}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)
        meetingID = intent.getStringExtra(MEETING_ID)!!
        isJoin = intent.getBooleanExtra(IS_JOIN,false)
        photoUrl = intent.getStringExtra(PHOTO_URL)!!
        uid = intent.getStringExtra(UID)!!
        displayName = intent.getStringExtra(DISPLAY_NAME)!!
        remoteView = findViewById(R.id.remote_view)
        localView = findViewById(R.id.local_view)
        endCallBtn = findViewById(R.id.end_call)

        signalingClient = SignalingClient(meetingID,createSignallingClientListener())

        rtcClient = RTCClient(
            application,
            object : PeerConnectionObserver(){
                override fun onIceCandidate(p0: IceCandidate?) {
                    super.onIceCandidate(p0)
                    signalingClient.sendIceCandidate(p0,isJoin)
                    rtcClient.addIceCandidate(p0)
                }

                override fun onAddStream(p0: MediaStream?) {
                    super.onAddStream(p0)
                    p0?.videoTracks?.get(0)?.addSink(remoteView)
                }
            }
        )

        rtcClient.initSurfaceView(remoteView)
        rtcClient.initSurfaceView(localView)
        rtcClient.startLocalVideoCapture(localView)

        if(!isJoin) rtcClient.call(sdpObserver,meetingID)

        endCallBtn.setOnClickListener {
            rtcClient.endCall(meetingID)
            startMainActivity()
        }
    }

    private fun startMainActivity(){
        val intent = Intent(this,MainActivity::class.java)
        intent.putExtra(RETURN_FROM_CALL,true)
        intent.putExtra(MEETING_ID,meetingID)
        intent.putExtra(UID,uid)
        intent.putExtra(PHOTO_URL,photoUrl)
        intent.putExtra(DISPLAY_NAME,displayName)
        startActivity(intent)
        finish()
        finish()
    }

    private fun createSignallingClientListener() = object : SignalingClientListener {
        override fun onConnectionEstablished() {
            endCallBtn.isClickable = true
        }

        override fun onOfferReceived(description: SessionDescription) {
            rtcClient.onRemoteSessionReceived(description)
            rtcClient.answer(sdpObserver,meetingID)
        }

        override fun onAnswerReceived(description: SessionDescription) {
            rtcClient.onRemoteSessionReceived(description)
        }

        override fun onIceCandidateReceived(iceCandidate: IceCandidate) {
            rtcClient.addIceCandidate(iceCandidate)
        }

        override fun onCallEnded() {
            rtcClient.endCall(meetingID)
            startMainActivity()
        }

    }

    override fun onDestroy() {
        signalingClient.destroy()
        super.onDestroy()
    }
}