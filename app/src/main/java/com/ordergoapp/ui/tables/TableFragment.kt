package com.ordergoapp.ui.tables

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.ordergoapp.R
import com.ordergoapp.databinding.FragmentTableBinding
import com.ordergoapp.service.data.OrderShape
import com.ordergoapp.service.data.ROSOrderItem
import com.ordergoapp.service.local.SessionManager
import com.ordergoapp.service.data.ROSUtils
import com.ordergoapp.service.datasource.FirebaseDataSource
import com.ordergoapp.service.repositry.ROSRepositry
import com.ordergoapp.ui.main.MainActivity
import com.ordergoapp.ui.adapter.TableAdapter
import com.ordergoapp.ui.completed_orders.OrderDetailsDialog
import com.ordergoapp.utils.Utils
import com.ordergoapp.utils.Variables
import com.ordergoapp.viewmodel.OrdersViewModel
import java.util.*
import kotlin.collections.ArrayList


class TableFragment : Fragment(), TableAdapter.OnItemClickListener {

    private lateinit var sessionManager: SessionManager
    private lateinit var binding: FragmentTableBinding
    private var nTable = 0;
    private lateinit var list: ArrayList<Int>
    private lateinit var placedOrdersViewModel: OrdersViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentTableBinding.inflate(layoutInflater, container, false)
        sessionManager = SessionManager(this.requireContext())
        list = ArrayList<Int>()

        Variables.isCompletedTabOpened = false

        binding.txtResID.text = sessionManager.fetchROS()?.let {
            getString(R.string.ros_restaurant, ROSUtils.getResName(it))
        }
        binding.txtVersionNo.text = "V ${Utils.getVersionName(requireContext())}"

        sessionManager.fetchResNTable()?.let {
            nTable = sessionManager.fetchResNTable()
        }

        placedOrdersViewModel = ViewModelProvider(this).get(OrdersViewModel::class.java)
        placedOrdersViewModel.assignRepositry(
            ROSRepositry(
                FirebaseDataSource(sessionManager),
                sessionManager
            )
        )

        getTableList()
        //initRecycler()

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        getLiveTablesNumber()
    }

    fun getLiveTablesNumber() {
        if (activity != null) {
            var list = OrdersViewModel.placedOrders

            this.activity?.let {
                list.observe(it, {

                    try {
                        if (list.value != null) {
                            var liveTables =
                                getTablesNumbers(list.value!!?.groupBy { it -> it.order?.tn })
                            //Log.e("TableOrders ", "TableOrders: ${liveTables.toString()}")
                            initRecycler(liveTables)
                        }
                    } catch (e: Exception) {
                    }
                })
            }
        }
    }

    private fun getTablesNumbers(groupBy: Map<Long?, List<OrderShape>>): ArrayList<Int> {
        val list = arrayListOf<Int>()
        for (item in groupBy) {
            list.add(item?.key?.toInt()!!)
        }
        return list
    }


    private fun getTableList() {
        var i: Int = 1;
        while (i <= nTable) {
            list.add(i)
            i++
        }
        //list = MutableList(n) {index -> v}
    }

    private fun initRecycler(liveTables: ArrayList<Int>) {

        if (this.activity != null) {
            binding.rcvTables.layoutManager = GridLayoutManager(context, 6)
            val badge =
                (activity as MainActivity).navView.getOrCreateBadge(R.id.navigation_tables)
            badge.maxCharacterCount = 3
            badge.isVisible = false

            this.activity?.let {
                try {
                    if (list != null && nTable > 0) {
                        badge.number = nTable
                        badge.isVisible = true
                    } else {
                        badge.isVisible = false
                        badge.clearNumber()
                    }
                    if (binding.rcvTables.adapter == null)
                        binding.rcvTables.apply {
                            if (list != null) {
                                adapter = TableAdapter(
                                    list!!,
                                    liveTables!!,
                                    requireContext(),
                                    this@TableFragment
                                )
                            }
                        }
                } catch (e: Exception) {
                }

            }
        }
    }

    override fun onChildClicked(tableId: Int, view: View) {
        //Used SupportFragmentManager
        /*val detailsFragment = TableOrderFragment.newInstance(tableId)
        activity?.let {
            it.supportFragmentManager?.beginTransaction()?.add(
                R.id.nav_host_fragment,
                detailsFragment
            )?.commit() //.addToBackStack(null)
        }*/

        //Used SupportFragmentManager
        /*val intent = Intent(requireContext(), TableOrdersActivity::class.java)
        intent.putExtra(Utils.INTENT_TABLE_ID, tableId)
        startActivity(intent)*/

        //Used Navigate Controller
        val bundle = bundleOf(Utils.INTENT_TABLE_ID to tableId)
        Navigation.findNavController(view).navigate(R.id.navigation_tableOrders, bundle)

    }
}