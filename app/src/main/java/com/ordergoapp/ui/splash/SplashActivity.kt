package com.ordergoapp.ui.splash

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import com.ordergoapp.databinding.ActivitySplashBinding
import com.ordergoapp.service.local.SessionManager
import com.ordergoapp.ui.main.MainActivity
import com.ordergoapp.ui.login.LoginActivity
import com.ordergoapp.ui.ros_setup.RosSetupActivity
import com.ordergoapp.utils.NetworkConnection
import com.ordergoapp.utils.Variables
import java.util.*
import kotlin.concurrent.timerTask

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding;
    private lateinit var sessionManager: SessionManager
    private lateinit var firebaseAuth: FirebaseAuth

    private var isLoggedIn: Boolean = false
    private var isROS: String = ""
    private var isROSType: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initUi()
        initData()

    }

    private fun initUi() {
        firebaseAuth = FirebaseAuth.getInstance()
        sessionManager = SessionManager(this.applicationContext)
    }

    private fun initData(){
        isLoggedIn = sessionManager.fetchIsLoggedIn()
        isROS = sessionManager.fetchROS()!!
        isROSType = sessionManager.fetchROSType()!!

        checkInternetConnectionListener()
    }

    override fun onStart() {
        super.onStart()
        if (NetworkConnection.checkForInternet(this)) {
            binding.includeTextNoInternet.txtNoInternetConnection.visibility = View.GONE
            delay()
        } else
            binding.includeTextNoInternet.txtNoInternetConnection.visibility = View.VISIBLE

    }

    @Suppress("DEPRECATION")
    fun delay() {
        Handler().postDelayed({
            navigateControllerByLoginStatus()
        }, 1000)
    }

    private fun navigateControllerByLoginStatus() {
        if (isLoggedIn && firebaseAuth.currentUser != null) {
            if (!isROS.isNullOrEmpty() && !isROSType.isNullOrEmpty()) {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            } else {
                val intent = Intent(this, RosSetupActivity::class.java)
                startActivity(intent)
            }
        } else {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    private fun checkInternetConnectionListener() {
        //Check Internet Connectivity Listener
        Variables.networkNotifyListeners.add(object : Variables.InterfaceNetworkNotify {
            override fun networkChange(old: Boolean, new: Boolean) {
                if (new) {
                    runOnUiThread {
                        binding.includeTextNoInternet.txtNoInternetConnection.visibility = View.GONE
                        delay()

                    }
                } else {
                    runOnUiThread {
                        binding.includeTextNoInternet.txtNoInternetConnection.visibility =
                            View.VISIBLE
                    }
                }
            }

        })
    }
}