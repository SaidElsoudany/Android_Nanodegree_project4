package com.udacity.project4.authentication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.observe
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.google.firebase.auth.FirebaseUser
import com.udacity.project4.FirebaseUserLiveData
import com.udacity.project4.R
import com.udacity.project4.locationreminders.RemindersActivity


/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */

class AuthenticationActivity : AppCompatActivity() {

    private lateinit var launcher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authentication)
        findViewById<Button>(R.id.login_btn).setOnClickListener{
            launchFirebaseUi()
        }
        FirebaseUserLiveData().observe(this){ user ->
            if (user != null) {
                startActivity(Intent(this@AuthenticationActivity, RemindersActivity::class.java))
                finish()

            }
        }
        launcher = registerForActivityResult(
            FirebaseAuthUIActivityResultContract()
        ) { result ->
            if(result.resultCode != Activity.RESULT_OK){
                Toast.makeText(this@AuthenticationActivity,"Failed",Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun launchFirebaseUi(){
        launcher.launch(getSignInIntent())
    }
    private fun getSignInIntent(): Intent {
        // Implement the create account and sign in using FirebaseUI, use sign in using email and sign in using Google
        // Give users the option to sign in / register with their email or Google account.
        // If users choose to register with their email,
        // they will need to create a password as well.
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(), AuthUI.IdpConfig.GoogleBuilder().build()
            // This is where you can provide more ways for users to register and
            // sign in.
        )

        // You must provide a custom layout XML resource and configure at least one
        // provider button ID. It's important that that you set the button ID for every provider
        // that you have enabled.
        // You must provide a custom layout XML resource and configure at least one
        // provider button ID. It's important that that you set the button ID for every provider
        // that you have enabled.
        val customLayout = AuthMethodPickerLayout.Builder(R.layout.auth_custom_layout)
            .setGoogleButtonId(R.id.sign_in_with_google)
            .setEmailButtonId(R.id.sign_in_with_email)
            .build()

        return AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .setTheme(R.style.AppTheme)
            .setAuthMethodPickerLayout(customLayout)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
