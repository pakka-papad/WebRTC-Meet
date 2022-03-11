package com.example.webrtcmeet.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.webrtcmeet.RetrofitInstance
import com.example.webrtcmeet.daos.UserDao
import com.example.webrtcmeet.models.User
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SignInViewModel: ViewModel() {
    var user: User? = null
    private val _isUserNULL: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val isUserNULL: StateFlow<Boolean> = _isUserNULL
    private val userDao = UserDao()
    fun addUser(firebaseUser: FirebaseUser){
        user = User(
            uid = firebaseUser.uid,
            photoUrl = firebaseUser.photoUrl.toString(),
            displayName = firebaseUser.displayName.toString()
        )
        if(user != null) {
            userDao.addUser(user!!)
            _isUserNULL.value = false
        }
    }
    fun getRandomUser(){
        viewModelScope.launch {
            val response = RetrofitInstance.api.getRandomUsers()
            if (response.isSuccessful){
                val resObj = response.body()
                if(resObj != null){
                    val resUser = resObj.results[0]
                    user = User(
                        uid = resUser.login.uuid,
                        photoUrl = resUser.picture.large,
                        displayName = resUser.name.title + ' ' + resUser.name.first + ' ' + resUser.name.last
                    )
                    _isUserNULL.value = false
                }
            }
        }
    }

}