package com.ordergoapp.ui.login

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.PhoneAuthCredential
import com.ordergoapp.service.repositry.LoginRepository

import com.ordergoapp.R
import com.ordergoapp.service.data.model.LoggedInUser

class LoginViewModel(private val loginRepository: LoginRepository) : ViewModel() {


    private val _loginForm = MutableLiveData<LoginFormState>()
    val loginFormState: LiveData<LoginFormState> = _loginForm


    lateinit var user: LoggedInUser

    private lateinit var phoneAuthCredential: PhoneAuthCredential;

    fun loginDataChanged(phone: String) {
        if (isPhoneValid(phone)) {
            _loginForm.value = LoginFormState(isDataValid = true)
        }
    }

    // A placeholder username validation check
    private fun isPhoneValid(phone: String): Boolean {
        Log.e("loginXXX", "Phone: " + phone)
        return if (phone.isNullOrEmpty()) {
            _loginForm.value = LoginFormState(phoneError = R.string.invalid_empty_phone)
            false
        } else if (phone.length < 10) {
            _loginForm.value = LoginFormState(phoneError = R.string.invalid_phone)
            false
        } else {
            phone.isNotBlank()
        }
    }


}