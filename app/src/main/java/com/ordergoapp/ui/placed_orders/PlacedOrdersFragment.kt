package com.ordergoapp.ui.placed_orders

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.drawable.AnimationDrawable
import android.opengl.Visibility
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Observer
import com.ordergoapp.R
import com.ordergoapp.databinding.FragmentPlacedOrdersBinding
import com.ordergoapp.service.local.SessionManager
import com.ordergoapp.service.data.OrderShape
import com.ordergoapp.service.data.ROSUtils
import com.ordergoapp.service.datasource.FirebaseDataSource
import com.ordergoapp.service.repositry.ROSRepositry
import com.ordergoapp.ui.adapter.OrderItemsAdapter
import com.ordergoapp.ui.main.MainActivity
import com.ordergoapp.ui.adapter.OrdersAdapter
import com.ordergoapp.viewmodel.OrdersViewModel
import com.ordergoapp.utils.Utils
import com.ordergoapp.utils.Variables
import java.util.*
import kotlin.collections.ArrayList

class PlacedOrdersFragment : Fragment(), OrdersAdapter.OnItemClickListener {

    private lateinit var binding: FragmentPlacedOrdersBinding
    private lateinit var placedOrdersViewModel: OrdersViewModel

    private final val TAG = "PlacedOrdersFragment "

    private var listener: SessionManager.NotifyNTableListener? = null
    private lateinit var sessionManager: SessionManager

    private lateinit var timer: Timer
    private lateinit var searchTxt: EditText
    private var tableNumber = "All"
    private lateinit var orderList: ArrayList<OrderShape>
    private var autoPrint: Boolean = false

    private lateinit var orderListTrueTMA: ArrayList<OrderShape>
    private lateinit var orderListFalseTMA: ArrayList<OrderShape>

    @SuppressLint("StringFormatInvalid")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentPlacedOrdersBinding.inflate(layoutInflater, container, false)

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
            (binding.recyclerViewPlacedOrders.adapter as OrdersAdapter).filter.filter(searchTxt.text)
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
                    (binding.recyclerViewPlacedOrders.adapter as OrdersAdapter).filter.filter("")
                    Utils.hideKeyboardFrom(requireContext(), searchTxt)
                }
            }

            override fun afterTextChanged(s: Editable?) {

            }

        })
    }

    private fun initData() {
        Variables.isCompletedTabOpened = false

        autoPrint = sessionManager.fetchAutoPrint()
        binding.autoPrintSwitch.isChecked = autoPrint

        binding.autoPrintSwitch.setOnCheckedChangeListener { _, isChecked ->
            sessionManager.saveAutoPrint(isChecked)

        }
        binding.txtResID.text = sessionManager.fetchROS()?.let {
            getString(R.string.ros_restaurant, ROSUtils.getResName(it))
        }
        binding.txtVersionNo.text = "V ${Utils.getVersionName(requireContext())}"

        //starAnimation()
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
        getTwoList()
    }

    private fun initRecycler() {
        if (activity != null) {
            var list = OrdersViewModel.placedOrders
            //binding.recyclerViewPlacedOrders.layoutManager = GridLayoutManager(context, 3)
            var badge =
                (requireActivity() as MainActivity).navView.getOrCreateBadge(R.id.navigation_placedOrders)
            badge.maxCharacterCount = 3
            badge.isVisible = false

            var isTakeAwayROS = false
            if (sessionManager.fetchROSType().equals("t"))
                isTakeAwayROS = true

            this.activity?.let {

                list.observe(it, Observer {
                    Log.e(TAG, "Observe: $list")
                    //binding.loading.visibility = View.VISIBLE
                    try {
                        showLoading(true)
                        val count =
                            list.value?.sortedBy { orderShape ->
                                orderShape?.dateTime
                            }!!.filter { orderShape ->
                                orderShape.rosOrder?.itemsList?.size!! > 0
                            }!!.size

                        if (list.value != null && count > 0) {
                            badge.number = count
                            badge.isVisible = true

                            binding.includeNoItem.lytNoData.visibility = View.GONE
                        } else {
                            badge.isVisible = false
                            badge.clearNumber()

                            binding.includeNoItem.lytNoData.visibility = View.VISIBLE
                        }

                        if (binding.recyclerViewPlacedOrders.adapter == null)
                            binding.recyclerViewPlacedOrders.apply {
                                Log.e(TAG, "Observe: adapter == null")
                                orderList = ArrayList()
                                if (list.value != null) {
                                    orderList.addAll(list.value?.filter { orderShape -> orderShape.rosOrder?.itemsList?.size!! > 0 }!!)
                                    //orderList.addAll(list.value?.filter { orderShape -> orderShape.order?.tn!! == 1L }!!)
                                }
                                adapter = OrdersAdapter(
                                    orderList,
                                    requireContext(),
                                    this@PlacedOrdersFragment, false, tableNumber, isTakeAwayROS
                                )
                            }
                        else {
                            Log.e(TAG, "Observe: adapter != null")
                            orderList.clear()
                            orderList.addAll(list.value?.filter { orderShape -> orderShape.rosOrder?.itemsList?.size!! > 0 }!!)
                            filterByTable(tableNumber)
                        }

                        showLoading(false)
                    } catch (e: Exception) {
                        Log.e(TAG, "Orders ${e.message}")
                    }
                    //binding.loading.visibility = View.GONE

                })
            }
        }

    }

    private fun getTwoList() {
        binding.loading.visibility = View.VISIBLE
        if (activity != null) {
            var listFalseTMA = OrdersViewModel.placedOrdersFalseTMA
            var listTrueTMA = OrdersViewModel.placedOrdersTrueTMA

            orderListFalseTMA = ArrayList()
            orderListTrueTMA = ArrayList()

            this.activity?.let {
                listFalseTMA.observe(it, {
                    if (listFalseTMA.value != null) {
                        this.orderListFalseTMA.addAll(listFalseTMA.value?.filter { orderShape -> orderShape.rosOrder?.itemsList?.size!! > 0 }!!)
                        initRecycler2()
                    }
                })

                listTrueTMA.observe(it, {
                    if (listTrueTMA.value != null) {
                        this.orderListTrueTMA.addAll(listTrueTMA.value?.filter { orderShape -> orderShape.rosOrder?.itemsList?.size!! > 0 }!!)
                        initRecycler2()
                    }
                })

            }
        }

        binding.loading.visibility = View.GONE
    }

    private fun initRecycler2() {
        if (activity != null) {

            orderList = ArrayList()
            if (orderList.isEmpty()) {
                orderList.addAll(orderListFalseTMA)
                orderList.addAll(orderListTrueTMA)
            } else {
                orderList.clear()
                orderList.addAll(orderListFalseTMA)
                orderList.addAll(orderListTrueTMA)
            }

            var badge =
                (requireActivity() as MainActivity).navView.getOrCreateBadge(R.id.navigation_placedOrders)
            badge.maxCharacterCount = 3
            badge.isVisible = false

            var isTakeAwayROS = false
            if (sessionManager.fetchROSType().equals("t"))
                isTakeAwayROS = true

            this.activity?.let {
                try {
                    if (orderList != null && orderList.size > 0) {
                        badge.number = orderList.size
                        badge.isVisible = true

                        binding.includeNoItem.lytNoData.visibility = View.GONE
                    } else {
                        badge.isVisible = false
                        badge.clearNumber()

                        binding.includeNoItem.lytNoData.visibility = View.VISIBLE
                    }

                    if (binding.recyclerViewPlacedOrders.adapter == null)
                        binding.recyclerViewPlacedOrders.apply {
                            Log.e(TAG, "Observe: adapter == null")
                            if (orderList != null) {
                                orderList.addAll(orderList?.filter { orderShape -> orderShape.rosOrder?.itemsList?.size!! > 0 }!!)
                            }
                            adapter = OrdersAdapter(
                                orderList,
                                requireContext(),
                                this@PlacedOrdersFragment, false, tableNumber, isTakeAwayROS
                            )
                        }
                    else {
                        Log.e(TAG, "Observe: adapter != null")
                        //orderList.clear()
                        //orderList.addAll(orderList?.filter { orderShape -> orderShape.rosOrder?.itemsList?.size!! > 0 }!!)
                        filterByTable(tableNumber)
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Orders ${e.message}")
                }

            }
        }

    }

    private fun filterByTable(tag: String) {
        try {
            tableNumber = tag
            val adapter = (binding.recyclerViewPlacedOrders.adapter as OrdersAdapter)
            adapter.setTableNumber(tag)
            adapter.setOrdersList(orderList)
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
        /*if (isShared)
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
//        try {
//            timer.cancel()
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
    }

    fun showLoading(state: Boolean) {
        if (state)
            Utils.showCustomLoadingDialog(requireContext());
        else
            Utils.hideCustomLoadingDialog();

    }

}
