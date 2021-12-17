package com.ordergoapp.ui.ros_setup

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ordergoapp.service.data.ROS
import com.ordergoapp.service.repositry.ROSRepositry

class ROSSetupViewModel(private val repositry: ROSRepositry) : ViewModel() {

    var rosList = MutableLiveData<List<ROS>>()
    fun getAllRos() {
      rosList=repositry.getAllROS()
    }
    fun saveSetupInfo(ros:ROS,context: Context)
    {
        repositry.saveSetupInfo(context,ros)
    }
    fun updateRosStatus(ros: ROS)
    {
        repositry.updateROSStatus(ROSRepositry.user?.restId!!,ros.id)
    }
}