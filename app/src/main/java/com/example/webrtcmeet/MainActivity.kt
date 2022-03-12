package com.example.webrtcmeet

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.webrtcmeet.Constants.ANSWER_CANDIDATE
import com.example.webrtcmeet.Constants.CALLS_COLLECTION
import com.example.webrtcmeet.Constants.CANDIDATES_COLLECTION
import com.example.webrtcmeet.Constants.DISPLAY_NAME
import com.example.webrtcmeet.Constants.IS_JOIN
import com.example.webrtcmeet.Constants.MEETING_ID
import com.example.webrtcmeet.Constants.OFFER_CANDIDATE
import com.example.webrtcmeet.Constants.PHOTO_URL
import com.example.webrtcmeet.Constants.RETURN_FROM_CALL
import com.example.webrtcmeet.Constants.UID
import com.example.webrtcmeet.Constants.USERS_COLLECTION
import com.example.webrtcmeet.adapters.CallListAdapter
import com.example.webrtcmeet.adapters.ICallListAdapter
import com.example.webrtcmeet.models.User
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MainActivity : AppCompatActivity(), ICallListAdapter{
    private lateinit var fab: FloatingActionButton
    private lateinit var recyclerView: RecyclerView
    private val requestCode: Int = 10
    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: CallListAdapter
    private var returnFromCall: Boolean = false
    private lateinit var uid: String
    private lateinit var photoUrl: String
    private lateinit var displayName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        db = FirebaseFirestore.getInstance()
        fab = findViewById(R.id.fab)
        recyclerView = findViewById(R.id.recycler_view)
        returnFromCall = intent.getBooleanExtra(RETURN_FROM_CALL,false)
        uid = intent.getStringExtra(UID)!!
        photoUrl = intent.getStringExtra(PHOTO_URL)!!
        displayName = intent.getStringExtra(DISPLAY_NAME)!!
        if (returnFromCall){
            val meetingID = intent.getStringExtra(MEETING_ID)
            db.collection(CALLS_COLLECTION).document(meetingID!!).collection(CANDIDATES_COLLECTION).document(ANSWER_CANDIDATE).delete()
            db.collection(CALLS_COLLECTION).document(meetingID).collection(CANDIDATES_COLLECTION).document(OFFER_CANDIDATE).delete()
            db.collection(CALLS_COLLECTION).document(meetingID).delete()
            db.collection(USERS_COLLECTION).document(meetingID).delete()
        }
        setupRecyclerView()
        fab.setOnClickListener {
            if(allPermissionsGranted()){
                val user = User(uid = uid,photoUrl = photoUrl,displayName = displayName)
                db.collection(USERS_COLLECTION).document(user.uid).set(user)
                    .addOnSuccessListener {
                        startCallActivity(user.uid,false)
                    }
            }
            else{
                requestPermission()

            }
        }
    }

    private fun requestPermission(){
        ActivityCompat.requestPermissions(this, requiredPermissions.toTypedArray(),requestCode)
    }

    private fun setupRecyclerView() {
        val usersCollection = db.collection(USERS_COLLECTION)
        val query = usersCollection.orderBy(UID,Query.Direction.DESCENDING)
        val recyclerOptions = FirestoreRecyclerOptions.Builder<User>().setQuery(query,User::class.java).build()
        adapter = CallListAdapter(recyclerOptions,this)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    override fun onStart() {
        super.onStart()
        adapter.startListening()
    }

    override fun onStop(){
        super.onStop()
        adapter.stopListening()
    }

    private fun startCallActivity(meetingID: String, isJoin: Boolean) {
        val intent = Intent(this,CallActivity::class.java)
        intent.putExtra(MEETING_ID,meetingID)
        intent.putExtra(IS_JOIN,isJoin)
        intent.putExtra(UID,uid)
        intent.putExtra(PHOTO_URL,photoUrl)
        intent.putExtra(DISPLAY_NAME,displayName)
        startActivity(intent)
        finish()
    }

    private fun allPermissionsGranted(): Boolean {
        for (permission in requiredPermissions){
            if( ContextCompat.checkSelfPermission(this,permission) != PackageManager.PERMISSION_GRANTED) return false
        }
        return true
    }

    companion object {
        val requiredPermissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.MODIFY_AUDIO_SETTINGS
        )
    }

    override fun onConnectClicked(uid: String) {
        if(allPermissionsGranted()){
            startCallActivity(uid,true)
        }
        else{
            requestPermission()
        }
    }
}