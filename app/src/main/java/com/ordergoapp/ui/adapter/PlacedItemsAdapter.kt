package com.ordergoapp.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ordergoapp.R
import com.ordergoapp.databinding.PlacedItemBinding
import com.ordergoapp.service.data.ROSOrderItem


class PlacedItemsAdapter(val items: List<ROSOrderItem>) :
    RecyclerView.Adapter<PlacedItemsAdapter.ViewHolder>() {


    class ViewHolder(val binding: PlacedItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun onBind(item: ROSOrderItem?) {
            binding.txtQty.text = item?.QTY.toString()
            binding.txtItemName.text = item?.name.toString()
            binding.txtItemServing.text = item?.serving?.capitalize().toString()
            binding.txtOrderID.text = item?.onId.toString()
            binding.txtTableID.text =
                if (item?.tableId == 0L) binding.root.resources.getString(R.string.takeAway) else item?.tableId.toString()

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            PlacedItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlacedItemsAdapter.ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.onBind(item)
    }

    override fun getItemCount(): Int = items.size


}