package com.ordergoapp.ui.completed_orders

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.ordergoapp.R
import com.ordergoapp.databinding.FragmentOrderDetailsDialogBinding
import com.ordergoapp.service.data.OrderShape
import com.ordergoapp.ui.adapter.OrderItemsAdapter
import com.ordergoapp.utils.Variables

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"

class OrderDetailsDialog : DialogFragment() {

    private var orderShape: OrderShape? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            orderShape = it.get(ARG_PARAM1) as OrderShape?

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout to use as dialog or embedded fragment
        val binding = FragmentOrderDetailsDialogBinding.inflate(layoutInflater, container, false)

        binding.txtTableID.text =
            if (orderShape?.order?.tn == 0L)
                getString(R.string.takeAway)
            else
                getString(R.string.table_num, orderShape?.order?.tn?.toString())
        binding.txtOrderID.text =
            getString(
                R.string.order_num,
                "${orderShape?.order?.on} "
            )

        binding.txtMobileNumber.text =
            if (orderShape?.order?.csm?.length!! > 4) {
                getString(
                    R.string.mobile_num,
                    "${
                        orderShape?.order!!.csm?.length?.let {
                            orderShape?.order!!.csm?.substring(
                                it - 4,
                                it
                            )
                        }
                    }"
                )
            } else {
                getString(
                    R.string.mobile_num,
                    "0000"
                )
            }
        binding.txtGuestNumber.text =
            if (orderShape?.order?.tn == 0L)
                binding.root.resources.getString(
                    R.string.guest_num,
                    "${Variables.placedOrdersCount} "
                )
            else
                binding.root.resources.getString(
                    R.string.guest_num,
                    "${orderShape?.guest?.total?.toString()}"
                )
        val type =
            OrderItemsAdapter.DetailedItem
        val childLayoutManager = LinearLayoutManager(
            context,
            LinearLayoutManager.VERTICAL,
            false
        )

        if (orderShape?.rosOrder != null) {
            childLayoutManager.initialPrefetchItemCount = orderShape?.rosOrder?.itemsList?.size!!

            val childItemAdapter = OrderItemsAdapter(
                orderShape?.rosOrder?.itemsList, null, type
            )
            binding.recyclerViewOrderItems.apply {
                layoutManager = layoutManager
                adapter = childItemAdapter
            }
        }
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @return A new instance of fragment BlankFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(orderShape: OrderShape) =
            OrderDetailsDialog().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_PARAM1, orderShape)

                }
            }
    }

}