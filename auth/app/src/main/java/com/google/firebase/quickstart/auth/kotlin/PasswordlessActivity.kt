package com.google.firebase.quickstart.auth.kotlin

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.text.TextUtils
import android.util.Log
import android.view.View
import com.google.firebase.auth.*
import com.google.firebase.quickstart.auth.R
import kotlinx.android.synthetic.main.activity_passwordless.*


/**
 * Demonstrate Firebase Authentication without a password, using a link sent to an
 * email address.
 */
class PasswordlessActivity : BaseActivity(), View.OnClickListener {

    private val TAG = "PasswordlessSignIn"
    private val KEY_PENDING_EMAIL = "key_pending_email"

    private var mPendingEmail: String = ""
    private var mEmailLink: String = ""
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_passwordless)

        mAuth = FirebaseAuth.getInstance()

        passwordlessSendEmailButton.setOnClickListener(this)
        passwordlessSignInButton.setOnClickListener(this)
        signOutButton.setOnClickListener(this)

        // Restore the "pending" email address
        if (savedInstanceState != null) {
            mPendingEmail = savedInstanceState.getString(KEY_PENDING_EMAIL, null)
            fieldEmail.setText(mPendingEmail)
        }

        // Check if the Intent that started the Activity contains an email sign-in link.
        checkIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        updateUI(mAuth.currentUser)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        checkIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_PENDING_EMAIL, mPendingEmail)
    }

    /**
     * Check to see if the Intent has an email link, and if so set up the UI accordingly.
     * This can be called from either onCreate or onNewIntent, depending on how the Activity
     * was launched.
     */
    private fun checkIntent(intent: Intent?) {
        if (intentHasEmailLink(intent)) {
            mEmailLink = intent!!.data!!.toString()

            status.setText(R.string.status_link_found)
            passwordlessSendEmailButton.isEnabled = false
            passwordlessSignInButton.isEnabled = true
        } else {
            status.setText(R.string.status_email_not_sent)
            passwordlessSendEmailButton.isEnabled = true
            passwordlessSignInButton.isEnabled = false
        }
    }

    /**
     * Determine if the given Intent contains an email sign-in link.
     */
    private fun intentHasEmailLink(intent: Intent?): Boolean {
        if (intent != null && intent.data != null) {
            val intentData = intent.data!!.toString()
            if (mAuth.isSignInWithEmailLink(intentData)) {
                return true
            }
        }

        return false
    }

    /**
     * Send an email sign-in link to the specified email.
     */
    private fun sendSignInLink(email: String) {
        val settings = ActionCodeSettings.newBuilder()
                .setAndroidPackageName(
                        packageName,
                        false, null/* minimum app version */)/* install if not available? */
                .setHandleCodeInApp(true)
                .setUrl("https://auth.example.com/emailSignInLink")
                .build()

        hideKeyboard(fieldEmail)
        showProgressDialog()

        mAuth.sendSignInLinkToEmail(email, settings)
                .addOnCompleteListener { task ->
                    hideProgressDialog()

                    if (task.isSuccessful) {
                        Log.d(TAG, "Link sent")
                        showSnackbar("Sign-in link sent!")

                        mPendingEmail = email
                        status.setText(R.string.status_email_sent)
                    } else {
                        val e = task.exception
                        Log.w(TAG, "Could not send link", e)
                        showSnackbar("Failed to send link.")

                        if (e is FirebaseAuthInvalidCredentialsException) {
                            fieldEmail.error = "Invalid email address."
                        }
                    }
                }
    }

    /**
     * Sign in using an email address and a link, the link is passed to the Activity
     * from the dynamic link contained in the email.
     */
    private fun signInWithEmailLink(email: String, link: String?) {
        Log.d(TAG, "signInWithLink:" + link!!)

        hideKeyboard(fieldEmail)
        showProgressDialog()

        mAuth.signInWithEmailLink(email, link)
                .addOnCompleteListener { task ->
                    hideProgressDialog()
                    if (task.isSuccessful) {
                        Log.d(TAG, "signInWithEmailLink:success")

                        fieldEmail.text = null
                        updateUI(task.result.user)
                    } else {
                        Log.w(TAG, "signInWithEmailLink:failure", task.exception)
                        updateUI(null)

                        if (task.exception is FirebaseAuthActionCodeException) {
                            showSnackbar("Invalid or expired sign-in link.")
                        }
                    }
                }
    }

    private fun onSendLinkClicked() {
        val email = fieldEmail.text.toString()
        if (TextUtils.isEmpty(email)) {
            fieldEmail.error = "Email must not be empty."
            return
        }

        sendSignInLink(email)
    }

    private fun onSignInClicked() {
        val email = fieldEmail.text.toString()
        if (TextUtils.isEmpty(email)) {
            fieldEmail.error = "Email must not be empty."
            return
        }

        signInWithEmailLink(email, mEmailLink)
    }

    private fun onSignOutClicked() {
        mAuth.signOut()

        updateUI(null)
        status.setText(R.string.status_email_not_sent)
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            status.text = getString(R.string.passwordless_status_fmt,
                    user.email, user.isEmailVerified)

            passwordlessFields.visibility = View.GONE
            passwordlessButtons.visibility = View.GONE
            signedInButtons.visibility = View.VISIBLE
        } else {
            passwordlessFields.visibility = View.VISIBLE
            passwordlessButtons.visibility = View.VISIBLE
            signedInButtons.visibility = View.GONE
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.passwordlessSendEmailButton -> onSendLinkClicked()
            R.id.passwordlessSignInButton -> onSignInClicked()
            R.id.signOutButton -> onSignOutClicked()
        }
    }
}
