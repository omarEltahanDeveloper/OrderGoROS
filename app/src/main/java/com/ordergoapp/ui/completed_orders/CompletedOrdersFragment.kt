package com.ordergoapp.ui.completed_orders

import android.annotation.SuppressLint
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.ordergoapp.R
import com.ordergoapp.databinding.FragmentCompletedOrdersBinding
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

class CompletedOrdersFragment : Fragment(), OrdersAdapter.OnItemClickListener {

    private lateinit var timer: Timer
    private lateinit var sessionManager: SessionManager
    private lateinit var searchTxt: EditText
    private lateinit var completedOrdersViewModel: OrdersViewModel
    private lateinit var binding: FragmentCompletedOrdersBinding
    private var tableNumber = "All"
    var startNumber = 1
    var endNumber = 1
    lateinit var orderList: ArrayList<OrderShape>

    @SuppressLint("StringFormatInvalid")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        try {
            completedOrdersViewModel =
                ViewModelProvider(this).get(OrdersViewModel::class.java)
            sessionManager = SessionManager(requireContext())
            completedOrdersViewModel.assignRepositry(
                ROSRepositry(
                    FirebaseDataSource(sessionManager),
                    sessionManager
                )
            )
            Variables.isCompletedTabOpened = false
            binding = FragmentCompletedOrdersBinding.inflate(layoutInflater, container, false)
            binding.txtResID.text = sessionManager.fetchROS()
                ?.let { getString(R.string.ros_restaurant, ROSUtils.getResName(it)) }
//            binding.imgPaid.visibility =
//                if (sessionManager.fetchPid()) View.VISIBLE else View.INVISIBLE

            //Search Functionality
            searchTxt = binding.searchTxt
            binding.btnSearch.setOnClickListener {
                (binding.recyclerViewCompletedOrders.adapter as OrdersAdapter).filter.filter(
                    searchTxt.text
                )
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
                        (binding.recyclerViewCompletedOrders.adapter as OrdersAdapter).filter.filter(
                            ""
                        )
                        Utils.hideKeyboardFrom(requireContext(), searchTxt)
                    }
                }

                override fun afterTextChanged(s: Editable?) {

                }

            })

            initRecycler()
            //  initTableNav()

            OrdersViewModel.restaurantInfo.observe(this.requireActivity(), {
//                binding.imgPaid.visibility = if (it.pid) View.VISIBLE else View.INVISIBLE
                if (!sessionManager.fetchROS().equals("t_1")) {
                    startNumber = 1
                    val nTable = sessionManager.fetchResNTable()
                    if (nTable > 0) {
                        endNumber = if (nTable > 10) 10 else nTable
//                        recreateTableNav(startNumber, endNumber)
                    }
                }
                //initRecycler()

            })

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
                                    binding?.layoutTitle?.background = drawable
                                    drawable?.start()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }

                            isFlashed = true
                        } else {
                            if (isFlashed) {
                                activity?.runOnUiThread {
                                    try {
                                        binding?.layoutTitle?.background =
                                            resources?.getDrawable(
                                                R.drawable.timer_rounded_background,
                                                null
                                            )
                                        drawable?.stop()
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
            binding.txtVersionNo.text = "V ${Utils.getVersionName(requireContext())}"

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            timer?.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initRecycler() {

        if (this.activity != null) {
            val list = OrdersViewModel.completedOrders
            binding.recyclerViewCompletedOrders.layoutManager = GridLayoutManager(context, 3)
            val badge =
                (activity as MainActivity).navView.getOrCreateBadge(R.id.navigation_completedOrders)
            badge.maxCharacterCount = 3
            badge.isVisible = false

            this.activity?.let {
                list.observe(it, {
                    try {
                        val count =
                            list.value?.filter { orderShape -> orderShape.rosOrder?.itemsList?.size!! > 0 }!!.size
                        if (list.value != null && count > 0) {
                            badge.number = count
                            badge.isVisible = true

                            binding.includeNoItem.lytNoData.visibility = View.GONE
                        } else {
                            badge.isVisible = false
                            badge.clearNumber()

                            binding.includeNoItem.lytNoData.visibility = View.VISIBLE
                        }
                        if (binding.recyclerViewCompletedOrders.adapter == null)
                            binding.recyclerViewCompletedOrders.apply {
                                if (list.value != null) {
                                    orderList = ArrayList<OrderShape>()
                                    orderList.addAll(list.value?.filter {
                                            orderShape -> orderShape.rosOrder?.itemsList?.size!! > 0
                                    }!!)

                                    adapter = OrdersAdapter(
                                        orderList!!,
                                        requireContext(),
                                        this@CompletedOrdersFragment, true
                                    )
                                }
                            }
                        else {
                            orderList.clear()
                            orderList.addAll(list.value?.filter { orderShape -> orderShape.rosOrder?.itemsList?.size!! > 0 }!!)
                            filterByTable(tableNumber)
//
                        }
                    } catch (e: Exception) {
                    }
                })
            }
        }
    }

    private fun filterByTable(tag: String) {
        tableNumber = tag
        val adapter = (binding.recyclerViewCompletedOrders.adapter as OrdersAdapter)
        adapter.setTableNumber(tag)
        adapter.filter.filter(searchTxt.text)
    }

    override fun onChildClicked(
        orderId: String,
        rosType: String,
        itemId: String,
        isShared: Boolean,
        sessionId: String
    ) {

    }

    override fun onChildCompleted(
        orderId: String,
        rosType: String,
        itemId: String,
        isShared: Boolean,
        sessionId: String
    ) {

    }

    override fun showDialog(orderShape: OrderShape) {
        val fragmentManager = activity?.supportFragmentManager
        val newFragment = OrderDetailsDialog.newInstance(orderShape)
        fragmentManager?.let { newFragment.show(it, "dialog") }
    }

    override fun printOrderCard(order: OrderShape, isAutoPrint: Boolean) {

    }
}