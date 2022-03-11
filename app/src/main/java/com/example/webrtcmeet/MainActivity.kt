package com.example.webrtcmeet

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.webrtcmeet.Constants.DISPLAY_NAME
import com.example.webrtcmeet.Constants.PHOTO_URL
import com.example.webrtcmeet.Constants.UID
import com.example.webrtcmeet.viewmodels.MainViewModel

class MainActivity : AppCompatActivity(){
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        viewModel = MainViewModel(this)
        val uid = intent.extras?.get(UID)
        val photoUrl = intent.extras?.get(PHOTO_URL)
        val displayName = intent.extras?.get(DISPLAY_NAME)
        viewModel.setUser(
            photoUrl = photoUrl.toString(),
            uid = uid.toString(),
            displayName = displayName.toString()
        )
    }
}