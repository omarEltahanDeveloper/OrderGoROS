package com.ordergoapp.ui.tables

import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.ordergoapp.R
import com.ordergoapp.databinding.FragmentTableOrderBinding
import com.ordergoapp.service.local.SessionManager
import com.ordergoapp.service.data.OrderShape
import com.ordergoapp.service.data.ROSUtils
import com.ordergoapp.service.datasource.FirebaseDataSource
import com.ordergoapp.service.repositry.ROSRepositry
import com.ordergoapp.ui.main.MainActivity
import com.ordergoapp.ui.adapter.OrdersAdapter
import com.ordergoapp.viewmodel.OrdersViewModel
import com.ordergoapp.utils.Utils
import com.ordergoapp.utils.Variables
import java.util.*
import kotlin.collections.ArrayList

class TableOrderFragment : Fragment(), OrdersAdapter.OnItemClickListener {

    private lateinit var sessionManager: SessionManager
    private lateinit var binding: FragmentTableOrderBinding
    private lateinit var placedOrdersViewModel: OrdersViewModel

    private var tableNumber = "All"
    private var tableId: Int = 0
    private lateinit var timer: Timer
    private lateinit var searchTxt: EditText
    lateinit var orderList: ArrayList<OrderShape>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            tableId = it.getInt(Utils.INTENT_TABLE_ID)
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentTableOrderBinding.inflate(layoutInflater, container, false)

        initUi()
        initViewModel()
        initUIAndActions()
        initData()

        return binding.root
    }

    private fun initUi() {
        sessionManager = SessionManager(this.requireContext())
        searchTxt = binding.searchTxt

    }

    private fun initViewModel() {
        placedOrdersViewModel = ViewModelProvider(this).get(OrdersViewModel::class.java)
        placedOrdersViewModel.assignRepositry(
            ROSRepositry(
                FirebaseDataSource(sessionManager),
                sessionManager
            )
        )
    }

    private fun initUIAndActions() {
        binding.btnSearch.setOnClickListener {
            (binding.rcvTableOrders.adapter as OrdersAdapter).filter.filter(searchTxt.text)
            Utils.hideKeyboardFrom(requireContext(), searchTxt)
        }

        searchTxt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrEmpty()) {
                    (binding.rcvTableOrders.adapter as OrdersAdapter).filter.filter("")
                    Utils.hideKeyboardFrom(requireContext(), searchTxt)
                }
            }

            override fun afterTextChanged(s: Editable?) {

            }

        })
    }

    private fun initData() {
        Variables.isCompletedTabOpened = false

        binding.txtResID.text = sessionManager.fetchROS()?.let {
            getString(R.string.ros_restaurant, ROSUtils.getResName(it))
        }
        binding.txtVersionNo.text = "V ${Utils.getVersionName(requireContext())}"

        binding.txtTableNo.text = "#${tableId}"

        starAnimation()
    }

    private fun starAnimation() {
        //region Animate
        val drawable = AnimationDrawable()

        drawable.addFrame(
            resources.getDrawable(R.drawable.timer_rounded_background, null),
            1000
        )
        drawable.addFrame(
            resources.getDrawable(R.drawable.timer_rounded_background_dark, null),
            1000
        )
        drawable.isOneShot = false


        timer = Timer("MetronomeTimer", true)
        var isFlashed: Boolean = false;

        val drawableTask = object : TimerTask() {
            override fun run() {
                //Play drawable
                try {
                    if (Variables.placedOrdersHasPlacedItems > 0) {
                        activity?.runOnUiThread {
                            try {
                                binding.layoutTitle.background = drawable
                                drawable.start()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        isFlashed = true
                    } else {
                        if (isFlashed) {
                            activity?.runOnUiThread {
                                try {
                                    binding.layoutTitle.background =
                                        resources.getDrawable(
                                            R.drawable.timer_rounded_background,
                                            null
                                        )
                                    drawable.stop()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }

                            }
                            isFlashed = false
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }

        }
        timer.scheduleAtFixedRate(drawableTask, 1000, 1000)
        //endregion
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecycler()
    }

    private fun initRecycler() {
        if (activity != null) {

            var list = OrdersViewModel.placedOrders

            var isTakeAwayROS = false
            if (sessionManager.fetchROSType().equals("t"))
                isTakeAwayROS = true

            this.activity?.let {
                list.observe(it, androidx.lifecycle.Observer {
                    try {
                        val count =
                            list.value?.filter { orderShape -> orderShape.order?.tn!! == tableId.toLong() }!!.size

                        if (list.value != null && count > 0) {
                            binding.includeNoItem.lytNoData.visibility = View.GONE
                        } else {
                            binding.includeNoItem.lytNoData.visibility = View.VISIBLE
                        }

                        if (binding.rcvTableOrders.adapter == null)
                            binding.rcvTableOrders.apply {
                                orderList = ArrayList()
                                if (list.value != null) {
                                    //orderList.addAll(list.value?.filter { orderShape -> orderShape.rosOrder?.itemsList?.size!! > 0 }!!)
                                    orderList.addAll(list.value?.filter { orderShape -> orderShape.order?.tn!! == tableId.toLong() }!!)
                                }
                                adapter = OrdersAdapter(
                                    orderList,
                                    requireContext(),
                                    this@TableOrderFragment, false, tableNumber, isTakeAwayROS
                                )
                            }
                        else {
                            orderList.clear()
                            //orderList.addAll(list.value?.filter { orderShape -> orderShape.rosOrder?.itemsList?.size!! > 0 }!!)
                            orderList.addAll(list.value?.filter { orderShape -> orderShape.order?.tn!! == tableId.toLong() }!!)
                            filterByTable(tableNumber)
                        }
                    } catch (e: Exception) {
                        Log.e("Orders: bf ", "${e.message}")
                    }
                })
            }
        }

    }

    private fun filterByTable(tag: String) {
        try {
            tableNumber = tag
            val adapter = (binding.rcvTableOrders.adapter as OrdersAdapter)
            adapter.setTableNumber(tag)
            adapter.filter.filter(searchTxt.text)
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
       /* if (isShared)
            placedOrdersViewModel.updateSharedItem(sessionId, itemId, "preparing")
        else
            placedOrdersViewModel.updateItem(orderId, rosType, itemId, "preparing")*/
        placedOrdersViewModel.updateOrderItemStatusFunctions(orderId, rosType, itemId, "preparing")

    }

    override fun onChildCompleted(
        orderId: String,
        rosType: String,
        itemId: String,
        isShared: Boolean, sessionId: String
    ) {
       /* if (isShared)
            placedOrdersViewModel.updateSharedItem(sessionId, itemId, "completed")
        else
            placedOrdersViewModel.updateItem(orderId, rosType, itemId, "completed")*/
        placedOrdersViewModel.updateOrderItemStatusFunctions(orderId, rosType, itemId, "completed")
    }


    override fun showDialog(orderShape: OrderShape) {

    }

    override fun printOrderCard(order: OrderShape, isAutoPrint: Boolean) {
        val result = (this.activity as MainActivity).printUsb(order, isAutoPrint)
        if (result)
            placedOrdersViewModel.setPrinted(order.orderId)
    }

    override fun onDestroy() {

        super.onDestroy()
        try {
            timer.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}