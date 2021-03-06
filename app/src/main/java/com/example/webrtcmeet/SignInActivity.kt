package com.example.webrtcmeet

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import com.example.webrtcmeet.Constants.ALL_USERS_COLLECTION
import com.example.webrtcmeet.Constants.DISPLAY_NAME
import com.example.webrtcmeet.Constants.ERROR_TEXT
import com.example.webrtcmeet.Constants.PHOTO_URL
import com.example.webrtcmeet.Constants.UID
import com.example.webrtcmeet.models.User
import com.example.webrtcmeet.viewmodels.SignInViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_LONG
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SignInActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInBtn: ConstraintLayout
    private lateinit var anonymousSignInBtn: ConstraintLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var loadingCard: CardView
    private lateinit var viewModel: SignInViewModel
    private val getContent =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    signInWithGoogle(account.idToken!!)
                } catch (e: Exception) {
                    showError()
                }
            } else {
                showError()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        googleSignInBtn = findViewById(R.id.google_sign_in_btn)
        anonymousSignInBtn = findViewById(R.id.anonymous_sign_in_btn)
        progressBar = findViewById(R.id.progress_bar)
        loadingCard = findViewById(R.id.loading_card)
        viewModel = SignInViewModel()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        auth = Firebase.auth
        googleSignInClient = GoogleSignIn.getClient(this, gso)


        googleSignInBtn.setOnClickListener {
            getContent.launch(Intent(googleSignInClient.signInIntent))
        }
        anonymousSignInBtn.setOnClickListener {
            fetchRandomUser()
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        updateUI(currentUser)
    }

    private fun signInWithGoogle(idToken: String) {
        loadingCard.visibility = View.VISIBLE
        progressBar.visibility = View.VISIBLE
        googleSignInBtn.isActivated = false
        anonymousSignInBtn.isActivated = false
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnSuccessListener {
            updateUI(it.user)
        }
    }

    private fun updateUI(firebaseUser: FirebaseUser?) {
        if (firebaseUser != null) {
            viewModel.addUser(firebaseUser)
            FirebaseFirestore.getInstance().collection(ALL_USERS_COLLECTION).document(firebaseUser.uid).set(User(firebaseUser.uid,firebaseUser.photoUrl.toString(),firebaseUser.displayName!!))
            launchMainActivity()
        } else {
            loadingCard.visibility = View.GONE
            progressBar.visibility = View.GONE
            googleSignInBtn.isActivated = true
            anonymousSignInBtn.isActivated = true
        }
    }

    private fun showError() {
        Snackbar.make(
            findViewById(R.id.sign_in_parent_layout),
            ERROR_TEXT,
            LENGTH_LONG
        ).show()
    }

    private fun fetchRandomUser() {
        loadingCard.visibility = View.VISIBLE
        progressBar.visibility = View.VISIBLE
        googleSignInBtn.isActivated = false
        anonymousSignInBtn.isActivated = false
        viewModel.getRandomUser()
        launchMainActivity()
    }

    private fun launchMainActivity() {
        lifecycleScope.launch {
            viewModel.isUserNULL.collectLatest {
                if (!it) {
                    val intent = Intent(this@SignInActivity, MainActivity::class.java)
                    intent.putExtra(DISPLAY_NAME,viewModel.user!!.displayName)
                    intent.putExtra(PHOTO_URL,viewModel.user!!.photoUrl)
                    intent.putExtra(UID,viewModel.user!!.uid)
                    startActivity(intent)
                    finish()
                }
            }
        }
    }
}