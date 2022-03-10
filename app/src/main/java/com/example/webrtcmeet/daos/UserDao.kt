package com.example.webrtcmeet.daos

import com.example.webrtcmeet.Constants.USERS_COLLECTION
import com.example.webrtcmeet.models.User
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class UserDao {
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection(USERS_COLLECTION)
    private val auth = Firebase.auth

    @OptIn(DelicateCoroutinesApi::class)
    fun addUser(user: User) {
        GlobalScope.launch(Dispatchers.IO) {
            usersCollection.document(user.uid).get().addOnSuccessListener {
                if (!it.exists()) usersCollection.document(user.uid).set(user)
            }
        }
    }
}