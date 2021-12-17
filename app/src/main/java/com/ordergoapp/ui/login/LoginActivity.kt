package com.ordergoapp.ui.login

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.ordergoapp.ui.main.MainActivity
import com.ordergoapp.R
import com.ordergoapp.databinding.ActivityLoginBinding
import com.ordergoapp.service.local.SessionManager
import com.ordergoapp.service.data.model.LoggedInUserView
import com.ordergoapp.service.repositry.ROSRepositry
import com.ordergoapp.ui.ros_setup.RosSetupActivity
import com.ordergoapp.utils.NetworkConnection
import com.ordergoapp.utils.Utils
import com.ordergoapp.utils.Variables
import java.lang.Exception
import java.util.concurrent.TimeUnit

class LoginActivity : AppCompatActivity(), View.OnClickListener {

    companion object {
        private val TAG = LoginActivity::class.java.name
        private const val KEY_VERIFY_IN_PROGRESS = "key_verify_in_progress"
        private const val KEY_TOKEN = "key_token"
        private const val KEY_PHONE = "key_phone"
        private const val KEY_VERIFICATION_ID = "key_verification_id"
        private const val STATE_INITIALIZED = 1
        private const val STATE_VERIFY_FAILED = 3
        private const val STATE_VERIFY_SUCCESS = 4
        private const val STATE_CODE_SENT = 2
        private const val STATE_SIGNIN_FAILED = 5
        private const val STATE_SIGNIN_SUCCESS = 6
    }

    private lateinit var binding: ActivityLoginBinding;
    private lateinit var loginViewModel: LoginViewModel
    private lateinit var sessionManager: SessionManager

    private lateinit var auth: FirebaseAuth
    private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var storedVerificationId: String? = ""
    private var phone: String? = ""

    private var verificationInProgress = false
    private var isLoggedIn = false;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Restore instance state
        if (savedInstanceState != null) {
            onRestoreInstanceState(savedInstanceState)
        }

        initUi()
        initViewModel()
        initUIAndActions()
        initData()

    }

    private fun initViewModel() {
        loginViewModel = ViewModelProvider(this, LoginViewModelFactory())
            .get(LoginViewModel::class.java)
    }

    private fun initUi() {
        sessionManager = SessionManager(this.applicationContext)
        // Initialize Firebase Auth
        auth = Firebase.auth
    }

    private fun initUIAndActions() {
        //Check Internet Connectivity Listener
        Variables.networkNotifyListeners.add(object : Variables.InterfaceNetworkNotify {
            override fun networkChange(old: Boolean, new: Boolean) {
                if (new) {
                    runOnUiThread {
                        binding.includeTextNoInternet.txtNoInternetConnection.visibility = View.GONE
                    }
                } else {
                    runOnUiThread {
                        binding.includeTextNoInternet.txtNoInternetConnection.visibility =
                            View.VISIBLE
                    }
                }
            }

        })

        // Assign click listeners
        binding.buttonStartVerification.setOnClickListener(this)
        binding.buttonVerifyPhone.setOnClickListener(this)
        binding.buttonResend.setOnClickListener(this)

        //CountryCode register to phone text
        binding.ccp.registerCarrierNumberEditText(binding.fieldPhoneNumber)

        binding.tvCountryCode.text =
            "+" + binding.ccp.selectedCountryCode.toString().trim();

        binding.ccp.setOnCountryChangeListener {
            binding.tvCountryCode.text =
                "+" + binding.ccp.selectedCountryCode.toString().trim();
        }

        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // This callback will be invoked in two situations:
                // 1 - Instant verification. In some cases the phone number can be instantly
                //     verified without needing to send or enter a verification code.
                // 2 - Auto-retrieval. On some devices Google Play services can automatically
                //     detect the incoming verification SMS and perform verification without
                //     user action.
                Log.d(TAG, "onVerificationCompleted:$credential")
                // [START_EXCLUDE silent]
                verificationInProgress = false
                // [END_EXCLUDE]

                // [START_EXCLUDE silent]
                // Update the UI and attempt sign in with the phone credential
                updateUI(STATE_VERIFY_SUCCESS, credential)
                // [END_EXCLUDE]
                signInWithPhoneAuthCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                // This callback is invoked in an invalid request for verification is made,
                // for instance if the the phone number format is not valid.
                Log.w(TAG, "onVerificationFailed", e)
                // [START_EXCLUDE silent]
                verificationInProgress = false
                // [END_EXCLUDE]

                if (e is FirebaseAuthInvalidCredentialsException) {
                    // Invalid request
                    // [START_EXCLUDE]
                    binding.fieldPhoneNumber.error = getString(R.string.invalid_phone)
                    // [END_EXCLUDE]
                } else if (e is FirebaseTooManyRequestsException) {
                    // The SMS quota for the project has been exceeded
                    // [START_EXCLUDE]
                    Snackbar.make(
                        findViewById(android.R.id.content), "Quota exceeded.",
                        Snackbar.LENGTH_SHORT
                    ).show()
                    // [END_EXCLUDE]
                }

                // Show a message and update the UI
                // [START_EXCLUDE]
                updateUI(STATE_VERIFY_FAILED)
                // [END_EXCLUDE]
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                // The SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.
                Log.d(TAG, "onCodeSent:$verificationId")

                // Save verification ID and resending token so we can use them later
                storedVerificationId = verificationId
                resendToken = token

                // [START_EXCLUDE]
                // Update UI
                updateUI(STATE_CODE_SENT)
                // [END_EXCLUDE]
            }
        }

    }

    private fun initData() {
        isLoggedIn = sessionManager.fetchIsLoggedIn()

        loginViewModel.loginFormState.observe(this@LoginActivity, Observer {
            val loginState = it ?: return@Observer

            // disable login button unless both username / password is valid
            binding.buttonStartVerification.isEnabled = loginState.isDataValid

            if (loginState.phoneError != null) {
                binding.fieldPhoneNumber.error = getString(loginState.phoneError)
            }

        })

        binding.fieldPhoneNumber.afterTextChanged {
            loginViewModel.loginDataChanged(
                binding.fieldPhoneNumber.text.toString().trim()
            )
        }

    }

    // [START on_start_check_user]
    override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        if (!isLoggedIn) {
            val currentUser = auth.currentUser
            //updateUI(STATE_INITIALIZED)

            // [START_EXCLUDE]
            if (verificationInProgress && validatePhoneNumber()) {
                //startPhoneNumberVerification()
            }
            // [END_EXCLUDE]
        }
    }

    // [END on_start_check_user]
    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            updateUI(STATE_SIGNIN_SUCCESS, user)
        } else {
            updateUI(STATE_INITIALIZED)
        }
    }

    private fun updateUI(uiState: Int, cred: PhoneAuthCredential) {
        updateUI(uiState, null, cred)
    }

    private fun updateUI(
        uiState: Int,
        user: FirebaseUser? = auth.currentUser,
        cred: PhoneAuthCredential? = null
    ) {
        when (uiState) {
            STATE_INITIALIZED -> {
                // Initialized state, show only the phone number field and start button
                showViews(
                    binding.buttonStartVerification,
                    binding.fieldPhoneNumber,
                    binding.layoutPhone
                )
                hideViews(
                    binding.buttonVerifyPhone,
                    binding.buttonResend,
                    binding.fieldVerificationCode,
                    binding.layoutVerifyPhone
                )

            }
            STATE_CODE_SENT -> {
                // Code sent state, show the verification field, the
                //binding.loading.visibility = View.GONE
                Utils.showLoading(false, this)
                countTimer()
                showViews(
                    binding.buttonVerifyPhone,
                    binding.buttonResend,
                    binding.fieldVerificationCode,
                    binding.layoutVerifyPhone
                )
                hideViews(
                    binding.layoutPhone,
                    binding.buttonStartVerification
                )
                binding.detail.setText(R.string.status_verification_succeeded)
            }
            STATE_VERIFY_FAILED -> {
                // Verification has failed, show all options
                //binding.loading.visibility = View.GONE
                Utils.showLoading(false, this)
                /*showViews(
                    binding.buttonStartVerification,
                    binding.buttonVerifyPhone,
                    binding.buttonResend,
                    binding.fieldPhoneNumber,
                    binding.fieldVerificationCode
                )*/
                binding.detail.setText(R.string.status_verification_failed)
            }
            STATE_VERIFY_SUCCESS -> {
                // Verification has succeeded, proceed to firebase sign in
                //binding.loading.visibility = View.GONE
                Utils.showLoading(false, this)
                /*hideViews(
                    binding.buttonStartVerification,
                    binding.buttonVerifyPhone,
                    binding.buttonResend,
                    binding.fieldPhoneNumber,
                    binding.fieldVerificationCode
                )*/
                binding.detail.setText(R.string.status_verification_succeeded)

            }
            STATE_SIGNIN_FAILED -> {
                // No-op, handled by sign-in check
                //binding.loading.visibility = View.GONE
                Utils.showLoading(false, this)
                binding.detail.setText(R.string.status_sign_in_failed)
            }
            STATE_SIGNIN_SUCCESS -> {
                //binding.loading.visibility = View.GONE
                Utils.showLoading(false, this)
            }
        } // Np-op, handled by sign-in check

        if (user == null) {
            // Signed out

        } else {
            // Signed in

        }
    }

    fun countTimer() {
        val timer = object : CountDownTimer(120000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.txtTimer.visibility = View.VISIBLE
                binding.buttonResend.visibility = View.GONE
                binding.txtTimer.text = (millisUntilFinished / 1000).toString()
            }

            override fun onFinish() {
                // binding.txtTimer.visibility = View.INVISIBLE
                binding.buttonResend.visibility = View.VISIBLE
            }
        }
        timer.start()
    }

    private fun validatePhoneNumber(): Boolean {
        val phoneNumber = binding.fieldPhoneNumber.text.toString()
        if (TextUtils.isEmpty(phoneNumber)) {
            binding.fieldPhoneNumber.error =getString(R.string.invalid_empty_phone)
            return false
        }

        return true
    }

    private fun showViews(vararg views: View) {
        for (v in views) {
            v.isEnabled = true
            v.visibility = View.VISIBLE
        }

    }

    private fun hideViews(vararg views: View) {
        for (v in views) {
            v.visibility = View.GONE
        }
    }

    private fun showLoginFailed(@StringRes errorString: Int) {
        Toast.makeText(applicationContext, errorString, Toast.LENGTH_SHORT).show()
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success")

                    val user = task.result?.user
                    val loggedInUserView = LoggedInUserView()
                    user?.getIdToken(false)?.addOnCompleteListener {
                        if (it.isSuccessful) {
                            Log.d(TAG, "signInWithRule:success")
                            try {
                                if (it.result?.claims?.containsKey("owner")!!
                                    && it.result?.claims?.get("owner") as Boolean
                                ) {

                                    loggedInUserView.owner =
                                        it.result?.claims?.get("owner") as Boolean;
                                    loggedInUserView.restId =
                                        it.result?.claims?.get("idRest") as String;

                                    // [START_EXCLUDE]
                                    updateUI(STATE_SIGNIN_SUCCESS, user)

                                    ROSRepositry.setLoggedInUser(loggedInUserView)
                                    sessionManager.saveIsLoggedIn(true)
                                    sessionManager.saveRESID(loggedInUserView.restId)

                                    var intent = Intent(this, RosSetupActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                } else {
                                    // Update UI
                                    updateUI(STATE_SIGNIN_FAILED)

                                    binding.detail.text =
                                        "You have to call admin to activate your account."
                                    sessionManager.saveIsLoggedIn(false)
                                    signOut()
                                }

                            } catch (e: Exception) {
                                binding.detail.text = it.exception?.message

                                // Update UI
                                updateUI(STATE_SIGNIN_FAILED)
                            }

                        } else {
                            Log.w(TAG, "signInRule:failure", it.exception)
                            binding.detail.text = it.exception?.message

                            // Update UI
                            updateUI(STATE_SIGNIN_FAILED)
                        }
                    }?.addOnFailureListener {
                        Log.w(TAG, "signInRule:failure", it)
                        binding.detail.text = it.message

                        // Update UI
                        updateUI(STATE_SIGNIN_FAILED)
                    }
                } else {
                    // Sign in failed, display a message and update the UI
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        // The verification code entered was invalid
                        // [START_EXCLUDE silent]
                        binding.fieldVerificationCode.error = "Invalid code."
                        // [END_EXCLUDE]
                    }
                    // [START_EXCLUDE silent]
                    // Update UI
                    updateUI(STATE_SIGNIN_FAILED)
                    // [END_EXCLUDE]
                }
            }
    }

    private fun SignInFailed(e: Exception) {
        Log.w(TAG, "signInWithCredential:failure", e)

        binding.detail.text = e?.message

        // [START_EXCLUDE silent]
        // Update UI
        updateUI(STATE_SIGNIN_FAILED)
        // [END_EXCLUDE]
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.buttonStartVerification -> {
                if (NetworkConnection.checkForInternet(this)) {
                    binding.includeTextNoInternet.txtNoInternetConnection.visibility = View.GONE
                    if (validatePhoneNumber()) {
                        startPhoneNumberVerification()
                    }
                } else
                    if (binding.includeTextNoInternet.txtNoInternetConnection.visibility != View.VISIBLE)
                        binding.includeTextNoInternet.txtNoInternetConnection.visibility =
                            View.VISIBLE

            }
            R.id.buttonVerifyPhone -> {
                if (NetworkConnection.checkForInternet(this)) {
                    binding.includeTextNoInternet.txtNoInternetConnection.visibility = View.GONE
                    val code = binding.fieldVerificationCode.text.toString()
                    if (TextUtils.isEmpty(code)) {
                        binding.fieldVerificationCode.error = "Cannot be empty."
                        return
                    }

                    verifyPhoneNumberWithCode(storedVerificationId, code)
                } else
                    if (binding.includeTextNoInternet.txtNoInternetConnection.visibility != View.VISIBLE)
                        binding.includeTextNoInternet.txtNoInternetConnection.visibility =
                            View.VISIBLE

            }
            R.id.buttonResend -> {
                if (NetworkConnection.checkForInternet(this)) {
                    binding.includeTextNoInternet.txtNoInternetConnection.visibility = View.GONE
                    if (validatePhoneNumber()) {
                        resendVerificationCode(
                            phone!!,
                            resendToken
                        )
                    }
                } else
                    if (binding.includeTextNoInternet.txtNoInternetConnection.visibility != View.VISIBLE)
                        binding.includeTextNoInternet.txtNoInternetConnection.visibility =
                            View.VISIBLE

            }
        }
    }

    private fun signOut() {
        auth.signOut()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_VERIFY_IN_PROGRESS, verificationInProgress)
        outState.putString(KEY_PHONE, phone)
        outState.putString(KEY_VERIFICATION_ID, storedVerificationId)
        outState.putParcelable(KEY_TOKEN, resendToken)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        verificationInProgress = savedInstanceState.getBoolean(KEY_VERIFY_IN_PROGRESS)
        storedVerificationId = savedInstanceState.getString(KEY_VERIFICATION_ID)
        phone = savedInstanceState.getString(KEY_PHONE)
        if (resendToken != null)
            resendToken = savedInstanceState.getParcelable<PhoneAuthProvider.ForceResendingToken>(KEY_TOKEN)
                        as PhoneAuthProvider.ForceResendingToken
    }

    private fun startPhoneNumberVerification() {
        //binding.loading.visibility = View.VISIBLE
        Utils.showLoading(true, this)
        phone =
            "+${binding.ccp.selectedCountryCode.trim()}${binding.fieldPhoneNumber.text.trim()}"
        // [START start_phone_auth]
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phone!!)   //binding.ccp.fullNumberWithPlus    // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(this)                 // Activity (for callback binding)
            .setCallbacks(callbacks)          // OnVerificationStateChangedCallbacks
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
        // [END start_phone_auth]

        verificationInProgress = true
    }

    private fun verifyPhoneNumberWithCode(verificationId: String?, code: String) {
        Utils.showLoading(true, this)
        // [START verify_with_code]
        val credential = PhoneAuthProvider.getCredential(verificationId!!, code)
        // [END verify_with_code]
        signInWithPhoneAuthCredential(credential)
    }

    // [START resend_verification]
    private fun resendVerificationCode(
        phoneNumber: String,
        token: PhoneAuthProvider.ForceResendingToken?
    ) {
        Utils.showLoading(true, this)
        val optionsBuilder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)       // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(this)                 // Activity (for callback binding)
            .setCallbacks(callbacks)          // OnVerificationStateChangedCallbacks
        if (token != null) {
            optionsBuilder.setForceResendingToken(token) // callback's ForceResendingToken
        }
        PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
    }
    // [END resend_verification]


    /**
     * Extension function to simplify setting an afterTextChanged action to EditText components.
     */
    fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
        this.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(editable: Editable?) {
                afterTextChanged.invoke(editable.toString())
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        })

    }
}