package com.ordergoapp.ui.login

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.ordergoapp.R
import org.junit.After
import org.junit.Before

import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class LoginActivityTest {


    @Before
    fun setUp() {
    }

    @After
    fun tearDown() {
    }

    fun testLogin(){
        val activityScenario = ActivityScenario.launch(LoginActivity::class.java)

        var phone = "+447768907885"
        val testVerificationCode = "123456"

       // onView(withId(R.id.container)).check(matches(isDisplayed()))

        /*onView(withId(R.id.fieldPhoneNumber)).perform(typeText(phone))
        Espresso.closeSoftKeyboard()

        onView(withId(R.id.buttonStartVerification)).perform(click())*/

        //onView(withId(R.id.layout_verifyPhone)).check(matches(withText("")))

    }


}