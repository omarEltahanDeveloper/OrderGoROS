package com.ordergoapp.ui.placed_items

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import com.ordergoapp.ui.main.MainActivity
import com.ordergoapp.R
import com.ordergoapp.databinding.FragmentPlacedItemsBinding
import com.ordergoapp.service.local.SessionManager
import com.ordergoapp.service.data.ROSOrderItem
import com.ordergoapp.service.data.model.ROSOrderItemTotal
import com.ordergoapp.service.data.ROSUtils
import com.ordergoapp.service.datasource.FirebaseDataSource
import com.ordergoapp.service.datasource.Status
import com.ordergoapp.service.repositry.ROSRepositry
import com.ordergoapp.viewmodel.OrdersViewModel
import com.ordergoapp.ui.adapter.PlacedItemsAdapter
import com.ordergoapp.ui.adapter.PlacedItemsTotalAdapter
import com.ordergoapp.ui.ros_setup.RosSetupActivity
import com.ordergoapp.utils.Utils
import com.ordergoapp.utils.Variables

class PlacedItemsFragment : Fragment() {

    private lateinit var binding: FragmentPlacedItemsBinding
    private lateinit var placedItemsViewModel: OrdersViewModel

    @SuppressLint("StringFormatInvalid")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Variables.isCompletedTabOpened = false
        placedItemsViewModel =
            ViewModelProvider(this).get(OrdersViewModel::class.java)
        val sessionManager = SessionManager(requireContext())
        placedItemsViewModel.assignRepositry(
            ROSRepositry(
                FirebaseDataSource(sessionManager),
                sessionManager
            )
        )
        binding = FragmentPlacedItemsBinding.inflate(layoutInflater, container, false)
        binding.txtResID.text = sessionManager.fetchROS()?.let {
            getString(R.string.ros_restaurant, ROSUtils.getResName(it))
        }
        binding.imgPaid.visibility = if (sessionManager.fetchPid()) View.VISIBLE else View.INVISIBLE
        OrdersViewModel.restaurantInfo.observe(this.requireActivity(), {
            binding.imgPaid.visibility = if (it.pid) View.VISIBLE else View.INVISIBLE
            initRecycler()
        })

        binding.lytRosType.setOnClickListener {
            val intent = Intent(requireContext(), RosSetupActivity::class.java)
            startActivity(intent)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.loading.visibility = View.VISIBLE
        initRecycler()
    }

    private fun initRecycler() {
        if (activity != null) {
            var list = OrdersViewModel.placedItems
            var badge =
                (activity as MainActivity).navView.getOrCreateBadge(R.id.navigation_placedItems)
            badge.maxCharacterCount = 3
            badge.isVisible = false

            this.activity?.let {
                list.observe(it, Observer {

                    when (it.status) {
                        Status.SUCCESS -> {

                            Log.e("Orders: bf ", "x ii2 ${it?.data?.size}")
                            try {
                                if (it.data != null && it.data?.size!! > 0) {
                                    badge.number = it.data?.sumOf { it -> it.QTY }?.toInt()!!
                                    badge.isVisible = true

                                    binding.tvNotOrders.visibility = View.GONE
                                    binding.tvNotTotalOrders.visibility = View.GONE
                                } else {
                                    badge.isVisible = false
                                    badge.clearNumber()

                                    binding.tvNotOrders.visibility = View.VISIBLE
                                    binding.tvNotTotalOrders.visibility = View.VISIBLE

                                }

                                if (binding.recyclerViewPlacedItems.adapter == null)
                                    binding.recyclerViewPlacedItems.apply {
                                        if (list.value != null)
                                            adapter = PlacedItemsAdapter(
                                                it.data!!
                                            )
                                    }
                                else {
                                    binding.recyclerViewPlacedItems.adapter = PlacedItemsAdapter(
                                        it.data!!
                                    )
                                    binding.recyclerViewPlacedItems.adapter?.notifyDataSetChanged()
                                }

                                if (binding.recyclerViewPlacedItemsTotal.adapter == null)
                                    binding.recyclerViewPlacedItemsTotal.apply {
                                        if (it.data != null)
                                            adapter = PlacedItemsTotalAdapter(
                                                getTotalItems(it.data!!?.groupBy { it -> it.name })
                                            )
                                    }
                                else {
                                    binding.recyclerViewPlacedItemsTotal.adapter =
                                        PlacedItemsTotalAdapter(
                                            getTotalItems(it.data!!?.groupBy { it -> it.name })
                                        )
                                    binding.recyclerViewPlacedItemsTotal.adapter?.notifyDataSetChanged()
                                }

                                binding.txtTotal.text =
                                    it.data?.sumOf { it -> it.QTY }.toString()

                            } catch (e: Exception) {
                            }

                            binding.loading.visibility = View.GONE
                        }
                        Status.ERROR -> {
                            Snackbar.make(
                                binding.lyt,
                                it.message ?: "An unknown error occured.",
                                Snackbar.LENGTH_LONG
                            ).show()
                            binding.loading.visibility = View.GONE
                        }
                        Status.LOADING -> {
                            binding.loading.visibility = View.VISIBLE
                        }
                    }

                })

            }
        }
    }

    private fun getTotalItems(map: Map<String, List<ROSOrderItem>>): List<ROSOrderItemTotal> {
        val list = arrayListOf<ROSOrderItemTotal>()
        for (item in map) {
            list.add(ROSOrderItemTotal(item.key, item.value.sumOf { it.QTY }))
        }
        return list
    }
}