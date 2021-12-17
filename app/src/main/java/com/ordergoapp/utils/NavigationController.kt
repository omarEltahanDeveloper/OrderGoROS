package com.ordergoapp.utils

import android.app.Activity
import android.content.Intent
import com.ordergoapp.ui.login.LoginActivity
import com.ordergoapp.ui.main.MainActivity
import com.ordergoapp.ui.ros_setup.RosSetupActivity
import com.ordergoapp.ui.splash.SplashActivity

class NavigationController {
    companion object{

        fun navigateToSplashActivity(activity: Activity){
            val intent = Intent(activity, SplashActivity::class.java)
            activity.startActivity(intent)
        }

        fun navigateToMainActivity(activity: Activity){
            val intent = Intent(activity, MainActivity::class.java)
            activity.startActivity(intent)
        }

        fun navigateToLoginActivity(activity: Activity){
            val intent = Intent(activity, LoginActivity::class.java)
            activity.startActivity(intent)
        }

        fun navigateToRosActivity(activity: Activity){
            val intent = Intent(activity, RosSetupActivity::class.java)
            activity.startActivity(intent)
        }


    }
}