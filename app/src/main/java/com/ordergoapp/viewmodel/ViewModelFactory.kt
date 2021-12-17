package com.ordergoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ordergoapp.service.local.SessionManager
import com.ordergoapp.service.datasource.FirebaseDataSource
import com.ordergoapp.service.repositry.ROSRepositry
import com.ordergoapp.ui.ros_setup.ROSSetupViewModel

/**
 * ViewModel provider factory to instantiate LoginViewModel.
 * Required given LoginViewModel has a non-empty constructor
 */
class ViewModelFactory(val sessionManager: SessionManager) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ROSSetupViewModel(
                repositry = ROSRepositry(
                    dataSource = FirebaseDataSource(sessionManager), sessionManager = sessionManager
                )
            ) as T

    }
}