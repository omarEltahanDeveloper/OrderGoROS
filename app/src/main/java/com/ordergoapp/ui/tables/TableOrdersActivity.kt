package com.ordergoapp.ui.tables

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.ordergoapp.R
import com.ordergoapp.databinding.ActivityTableOrdersBinding
import com.ordergoapp.service.local.SessionManager
import com.ordergoapp.service.data.OrderShape
import com.ordergoapp.service.data.ROSOrderItem
import com.ordergoapp.service.data.ROSUtils
import com.ordergoapp.service.datasource.FirebaseDataSource
import com.ordergoapp.service.repositry.ROSRepositry
import com.ordergoapp.ui.adapter.OrdersAdapter
import com.ordergoapp.viewmodel.OrdersViewModel
import com.ordergoapp.utils.Utils
import com.ordergoapp.utils.Variables

class TableOrdersActivity : AppCompatActivity(), OrdersAdapter.OnItemClickListener {

    private lateinit var sessionManager: SessionManager
    private lateinit var binding: ActivityTableOrdersBinding
    private var tableId: Int = 0
    private lateinit var placedOrdersViewModel: OrdersViewModel
    private var tableNumber = "All"
    lateinit var orderList: ArrayList<OrderShape>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTableOrdersBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.loading.visibility = View.VISIBLE
        binding.cardLayout.visibility = View.GONE

        Variables.isCompletedTabOpened = false

        sessionManager = SessionManager(this)
        placedOrdersViewModel = ViewModelProvider(this).get(OrdersViewModel::class.java)

        placedOrdersViewModel.assignRepositry(
            ROSRepositry(
                FirebaseDataSource(sessionManager),
                sessionManager
            )
        )

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

        if (intent != null) {
            tableId = intent.getIntExtra(Utils.INTENT_TABLE_ID, 0)
        }


        binding.txtResID.text = sessionManager.fetchROS()?.let {
            getString(R.string.ros_restaurant, ROSUtils.getResName(it))
        }
        binding.txtVersionNo.text = "V ${Utils.getVersionName(this)}"

        binding.txtTableNo.text = tableId.toString()

        if (tableId == 0){
            finish()
            Toast.makeText(this,"Unknown error, try again..",Toast.LENGTH_LONG ).show()
        }


    }

    override fun onStart() {
        super.onStart()
        initRecycler()
    }


    private fun initRecycler() {
        val list = placedOrdersViewModel.getTableOrders(tableId)

        binding.rcvTableOrders.layoutManager = GridLayoutManager(this, 3)

        var isTakeAwayROS = false
        if (sessionManager.fetchROSType().equals("t"))
            isTakeAwayROS = true

        list.observe(this, {
            try {

                val count =
                    list.value?.filter { orderShape -> orderShape.rosOrder?.itemsList?.size!! > 0 }!!.size
                Log.e("TableOrder:  ", "${count}")

                if (list.value != null && count > 0) {
                    binding.tvNotOrders.visibility = View.GONE
                } else {
                    binding.tvNotOrders.visibility = View.VISIBLE
                }

                if (binding.rcvTableOrders.adapter == null)
                    binding.rcvTableOrders.apply {
                        orderList = ArrayList()
                        if (list.value != null) {
                            orderList.addAll(list.value?.filter { orderShape -> orderShape.rosOrder?.itemsList?.size!! > 0 }!!)
                        }
                        adapter = OrdersAdapter(
                            orderList,
                            this@TableOrdersActivity,
                            this@TableOrdersActivity,
                            false, tableNumber, isTakeAwayROS
                        )
                    }
                else {
                    orderList.clear()
                    orderList.addAll(list.value?.filter { orderShape -> orderShape.rosOrder?.itemsList?.size!! > 0 }!!)
                    filterByTable(tableNumber)
                }
            } catch (e: Exception) {
            }
            binding.loading.visibility = View.GONE
            binding.cardLayout.visibility = View.VISIBLE
        })


    }

    private fun filterByTable(tag: String) {
        try {
            tableNumber = tag
            val adapter = (binding.rcvTableOrders.adapter as OrdersAdapter)
            adapter.setTableNumber(tag)
            //adapter.filter.filter(searchTxt.query)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onChildClicked(
        orderId: String,
        rosType: String,
        itemId: String,
        isShared: Boolean, sessionId: String
    ) {
        Log.e("Status:  ", "onChildClicked $isShared $orderId")
//        if (isShared)
//            placedOrdersViewModel.updateSharedItem(sessionId, itemId, "preparing")
//        else
//            placedOrdersViewModel.updateItem(orderId, rosType, itemId, "preparing")
    }

    override fun onChildCompleted(
        orderId: String,
        rosType: String,
        itemId: String,
        isShared: Boolean, sessionId: String
    ) {

//        if (isShared)
//            placedOrdersViewModel.updateSharedItem(sessionId, itemId, "completed")
//        else
//            placedOrdersViewModel.updateItem(orderId, rosType, itemId, "completed")


    }


    override fun showDialog(orderShape: OrderShape) {

    }

    override fun printOrderCard(order: OrderShape, isAutoPrint: Boolean) {
        //val result = (this.activity as MainActivity).printUsb(order, isAutoPrint)
        // if (result)
        //    placedOrdersViewModel.setPrinted(order.orderId)
    }
}