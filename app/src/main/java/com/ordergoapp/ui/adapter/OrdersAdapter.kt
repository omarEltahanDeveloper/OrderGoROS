package com.ordergoapp.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ddd.androidutils.DoubleClick
import com.ddd.androidutils.DoubleClickListener
import com.ordergoapp.R
import com.ordergoapp.databinding.OrderCardBinding
import com.ordergoapp.service.local.SessionManager
import com.ordergoapp.service.data.OrderShape
import com.ordergoapp.service.datasource.FirebaseDataSource
import com.ordergoapp.utils.Variables


class OrdersAdapter(
    private var parents: List<OrderShape>,
    val context: Context,
    val parentListener: OnItemClickListener?,
    val completedOrders: Boolean,
    val tableNumberStr: String = "All",
    val isTakeawyROS: Boolean = false

) : RecyclerView.Adapter<OrdersAdapter.ViewHolder>(), Filterable {
    //   private val viewPool = RecyclerView.RecycledViewPool()
    private var filteredParents: List<OrderShape> = parents
    private var tableNumber = tableNumberStr;

     /*   init {
        filteredParents = if(tableNumber!="All") {
            filterByTableNumber()
        } else
            parents
    }*/

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val binding =
            OrderCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return filteredParents.size
    }

    @SuppressLint("SimpleDateFormat", "StringFormatMatches")
    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val parent = filteredParents[position]
        with(holder)
        {
            when (completedOrders) {
                true -> {
                    binding.imgPrint.visibility = View.GONE;
                }
                false -> {
                    binding.imgPrint.visibility = View.VISIBLE;

                    if (isTakeawyROS) {
                            binding.imgPrint.isEnabled = true
                            binding.imgPrint.alpha = 1F
                            if (parent.printed.isNullOrEmpty()
                               // && parent.docStatus == FirebaseDataSource.NEWDOC
                            ) {
                                parentListener?.printOrderCard(parent, true)
                            }
                    }
                    else {
                        if (SessionManager.autoPrint) {
                            binding.imgPrint.isEnabled = true
                            binding.imgPrint.alpha = 1F
                            if (parent.printed.isNullOrEmpty()
                                //&& parent.docStatus == FirebaseDataSource.NEWDOC
                            ) {
                                parentListener?.printOrderCard(parent, true)
                            }
                        }
                    }

                    binding.imgPrint.setOnClickListener {
                        parentListener?.printOrderCard(parent, false)
                    }

                    if (parent.rosOrder?.itemsList?.count {
                            if (it.status?.preparing != null)
                                it.status.preparing!!.isNotEmpty()
                            else
                                false
                        }!! > 0) {
                        /*binding.container.setBackgroundResource(R.color.white)
                        binding.cardViewOrder.setCardBackgroundColor(
                            binding.root.resources.getColor(
                                R.color.primary_dark,
                                null
                            )
                        )*/

                    } else {
                       /* binding.container.setBackgroundResource(R.color.primary_dark)
                        binding.cardViewOrder.setCardBackgroundColor(
                            binding.root.resources.getColor(
                                R.color.primary_dark,
                                null
                            )
                        )*/

                    }
                }
            }

            binding.txtOrderID.text =
                binding.root.resources.getString(
                    R.string.order_num,
                    "${parent?.order?.on}"
                )

            binding.txtMobileNumber.text =
                if (parent.order?.csm?.length!! > 4) {
                    binding.root.resources.getString(
                        R.string.mobile_num,
                        parent.order.csm.substring(
                            parent.order.csm.length - 4,
                            parent.order.csm.length
                        )
                    )
                } else {
                    binding.root.resources.getString(
                        R.string.mobile_num,
                        "0000"
                    )
                }
            binding.txtTableID.text =
                if (parent.order.tn == 0L)
                    binding.root.resources.getString(R.string.takeAway)
                else
                    binding.root.resources.getString(R.string.table_num, parent.order.tn)

            binding.txtGuestNumber.text =
                if (parent.order.tn == 0L)
                    binding.root.resources.getString(
                        R.string.guest_num,
                        "${Variables.placedOrdersCount}"
                    )
                else
                    binding.root.resources.getString(R.string.guest_num, "${parent.guest?.total}")
        }
        if (completedOrders) {
            val doubleClick = DoubleClick(object : DoubleClickListener {

                override fun onSingleClickEvent(view: View?) = Unit


                override fun onDoubleClickEvent(view: View?) {
                    parent.let { parentListener?.showDialog(it) }
                }
            })
            holder.itemView.setOnClickListener(doubleClick)
        }

        //Order items List
        val type =
            if (completedOrders) OrderItemsAdapter.CompletedItem else OrderItemsAdapter.PlacedItem

        val childLayoutManager = LinearLayoutManager(
            context,
            LinearLayoutManager.VERTICAL,
            false
        )

        if (parent.rosOrder != null) {
            childLayoutManager.initialPrefetchItemCount = parent.rosOrder?.itemsList?.size!!

            val childItemAdapter = OrderItemsAdapter(
                parent.rosOrder?.itemsList, holder, type
            )
            holder.binding.recyclerViewOrderItems.apply {
                layoutManager = layoutManager
                adapter = childItemAdapter
                //  setRecycledViewPool(viewPool)
            }
        }

    }


    inner class ViewHolder(val binding: OrderCardBinding) :
        RecyclerView.ViewHolder(binding.root), OnChildClickListener {
        override fun onChildClicked(
            rosType: String,
            uid: String,
            isShared: Boolean,
            sessionId: String
        ) {
            if (completedOrders) {
                filteredParents[layoutPosition].let { parentListener?.showDialog(it) }
            }
            else {
                //changeViewColors(binding)
                filteredParents[layoutPosition].orderId?.let {
                    parentListener?.onChildClicked(
                        it,
                        rosType,
                        uid, isShared, sessionId
                    )
                }
                //notifyDataSetChanged()
            }
        }

        override fun onChildCompleted(
            rosType: String,
            uid: String,
            isShared: Boolean,
            sessionId: String
        ) {
            // binding.root.alpha = 0.75F
            filteredParents[layoutPosition].orderId?.let {
                parentListener?.onChildCompleted(
                    it,
                    rosType,
                    uid, isShared, sessionId
                )
            }
            //notifyDataSetChanged()
        }

        private fun changeViewColors(binding: OrderCardBinding) {
            binding.container.setBackgroundResource(R.color.white)
            binding.cardViewOrder.setCardBackgroundColor(
                binding.root.resources.getColor(
                    R.color.primary_dark,
                    null
                )
            )
        }
    }

    interface OnChildClickListener {
        fun onChildCompleted(rosType: String, uid: String, isShared: Boolean, sessionId: String)
        fun onChildClicked(rosType: String, uid: String, isShared: Boolean, sessionId: String)
    }

    interface OnItemClickListener {
        fun onChildClicked(
            orderId: String,
            rosType: String,
            itemId: String,
            isShared: Boolean,
            sessionId: String
        )

        fun onChildCompleted(
            orderId: String,
            rosType: String,
            itemId: String,
            isShared: Boolean,
            sessionId: String
        )

        fun showDialog(orderShape: OrderShape)
        fun printOrderCard(order: OrderShape, isAutoPrint: Boolean)
        //fun showUserMessage(message: String)
    }

    fun setTableNumber(tableNum: String) {
        tableNumber = tableNum
    }

    fun setOrdersList(ordersList: List<OrderShape>) {
        parents = ordersList
        notifyDataSetChanged()
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val charSearch = constraint.toString()
                filteredParents = if (charSearch.isEmpty()) {
                    filterByTableNumber()

                } else {
                    val resultList = ArrayList<OrderShape>()

                    when (tableNumber) {
                        "All" -> {
                            for (row in parents) {
                                if (row.order?.on == charSearch.toLong()) {
                                    resultList.add(row)
                                }
                            }
                        }
                        "Preparing" -> {
                            for (row in parents) {
                                if (row.order?.on == charSearch.toLong() && hasPreparingItem(row)) {
                                    resultList.add(row)
                                }
                            }

                        }
                        "Placed" -> {
                            for (row in parents) {
                                if (row.order?.on == charSearch.toLong() && !hasPreparingItem(row)) {
                                    resultList.add(row)
                                }
                            }

                        }
                        else -> {
                            val tableNum = tableNumber.toLong()
                            for (row in parents) {
                                if (row.order?.on == charSearch.toLong() && row?.order?.tn == tableNum) {
                                    resultList.add(row)
                                }
                            }
                        }
                    }

                    resultList
                }
                val filterResults = FilterResults()
                filterResults.values = filteredParents
                return filterResults
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                try {
                    filteredParents = results?.values as ArrayList<OrderShape>
                    notifyDataSetChanged()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

        }

    }

    private fun filterByTableNumber(): List<OrderShape> {
        val resultList = ArrayList<OrderShape>()
        return when (tableNumber) {
            "All" -> parents
            "Preparing" -> {
                for (row in parents)
                    if (hasPreparingItem(row))
                        resultList.add(row)
                resultList
            }
            "Placed" -> {
                for (row in parents)
                    if (!hasPreparingItem(row))
                        resultList.add(row)
                resultList
            }
            else -> {
                val tableNum = tableNumber.toLong()
                for (row in parents)
                    if (row?.order?.tn == tableNum)
                        resultList.add(row)
                resultList
            }

        }
    }

    private fun hasPreparingItem(row: OrderShape): Boolean {
        for (item in row?.rosOrder?.itemsList!!) {
            if (!item.status?.preparing.isNullOrEmpty())
                return true
        }
        return false
    }
}