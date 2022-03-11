package com.example.webrtcmeet.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import com.example.webrtcmeet.models.User

class MainViewModel(context: Context): ViewModel(){
    private var user: User? = null

    fun setUser(photoUrl: String, uid: String, displayName: String){
        if(user == null){
            user = User(
                photoUrl = photoUrl,
                displayName = displayName,
                uid = uid
            )
        }
    }
}
