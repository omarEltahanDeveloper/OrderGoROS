package com.ordergoapp.ui.ros_setup

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.functions.FirebaseFunctions
import com.google.gson.Gson
import com.ordergoapp.R
import com.ordergoapp.ui.main.MainActivity
import com.ordergoapp.databinding.ActivityRosSetupBinding
import com.ordergoapp.service.local.SessionManager
import com.ordergoapp.service.data.ROS
import com.ordergoapp.service.repositry.ROSRepositry
import com.ordergoapp.viewmodel.ViewModelFactory
import com.ordergoapp.utils.NetworkConnection
import com.ordergoapp.utils.Utils
import com.ordergoapp.utils.Variables

class RosSetupActivity : AppCompatActivity() {
    private val TAG = "RosSetupActivity "
    private lateinit var binding: ActivityRosSetupBinding;
    private lateinit var viewModel: ROSSetupViewModel
    private var arrayAdapter: ArrayAdapter<ROS>? = null

    private lateinit var backPressedToast: Toast;
    private var backPressedTime: Long = 0;

    private var list: ArrayList<ROS>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRosSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        /*viewModel.getAllRos()
         viewModel.rosList.observe(this, { it ->
             if (list == null) {
                 list = arrayListOf<ROS>()
                 list?.addAll(it)
                 arrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, list!!)
                 binding.spinnerRos.adapter = arrayAdapter
             } else {
                 list?.clear()
                 list?.addAll(it)
                 arrayAdapter?.notifyDataSetChanged()
             }
         })*/


        initUi()
        initActions()

    }

    private fun initUi() {
        viewModel = ViewModelProvider(this, ViewModelFactory(SessionManager(this)))
            .get(ROSSetupViewModel::class.java)

        //Check Internet Connectivity
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

    }

    private fun initActions() {
        binding.btnSave.setOnClickListener {
            if (NetworkConnection.checkForInternet(this)) {
                binding.includeTextNoInternet.txtNoInternetConnection.visibility = View.GONE
                Utils.showLoading(true, this)
                saveSubmitClicked()
            } else
                binding.includeTextNoInternet.txtNoInternetConnection.visibility = View.VISIBLE

        }

        binding.swipeRefresh.setOnRefreshListener {
            getAllRos()
        }

    }

    private fun saveSubmitClicked(){
        if (binding.spinnerRos.selectedItem != null) {
            val ros = binding.spinnerRos.selectedItem as ROS
            //viewModel.updateRosStatus(ros)
            UpdateROSStatusByFunction(ros)
            viewModel.saveSetupInfo(ros, this)
            Utils.showLoading(false, this)
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onBackPressed() {
        backPressedToast = Toast.makeText(this, R.string.message_back_pressed, Toast.LENGTH_LONG)

        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            backPressedToast.cancel()
            //super.onBackPressed()
            finishAffinity()
            return
        } else {
            backPressedToast.show();
        }
        backPressedTime = System.currentTimeMillis();
    }

    override fun onStart() {
        super.onStart()
        getAllRos();
    }

    private fun getAllRos() {
        binding.lytLoadingBar.loading.visibility = View.VISIBLE
        if (NetworkConnection.checkForInternet(this)) {
            binding.includeTextNoInternet.txtNoInternetConnection.visibility = View.GONE
            getAllRosByFunctions();
        } else
            binding.includeTextNoInternet.txtNoInternetConnection.visibility = View.VISIBLE

    }

    private fun getAllRosByFunctions() {

        val map = hashMapOf(
            "restId" to ROSRepositry.user?.restId!!,
            "status" to false
        )
        FirebaseFunctions.getInstance("europe-west2")
            .getHttpsCallable("GetRestaurantROSsCF")
            .call(map)
            .continueWith { task ->
                if (task.exception != null) {
                    Log.w(TAG, "listen:error", task.exception)
                    return@continueWith
                }
                val result = task.result?.data
                Log.w(TAG, "listenROS: ${result.toString()}")
                parseDataAndSave(result)
                result
            }

    }

    private fun parseDataAndSave(result: Any?) {
        val json = Gson().toJson(result)
        val objectList: List<ROS> = Gson().fromJson(json, Array<ROS>::class.java).asList()
        Log.e(TAG, "Parse: ${objectList}")

        if (list == null) {
            list = arrayListOf<ROS>()
            list?.addAll(objectList)
            arrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, list!!)
            binding.spinnerRos.adapter = arrayAdapter
        } else {
            list?.clear()
            list?.addAll(objectList)
            arrayAdapter?.notifyDataSetChanged()
        }

        binding.lytLoadingBar.loading.visibility = View.GONE

    }

    private fun UpdateROSStatusByFunction(ros: ROS) {
        val map = hashMapOf(
            "restId" to ROSRepositry.user?.restId!!,
            "rosId" to ros.id,
            "status" to true
        )
        FirebaseFunctions.getInstance("europe-west2")
            .getHttpsCallable("UpdateStatusROSCF")
            .call(map)
            .continueWith { task ->
                if (task.exception != null) {
                    Log.w(TAG, "listen:error", task.exception)
                    return@continueWith
                }
                val result = task.result?.data
                result
            }
    }
}


