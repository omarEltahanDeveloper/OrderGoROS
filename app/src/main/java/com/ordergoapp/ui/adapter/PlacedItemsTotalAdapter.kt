package com.ordergoapp.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ordergoapp.databinding.PlacedItemTotalBinding
import com.ordergoapp.service.data.model.ROSOrderItemTotal


class PlacedItemsTotalAdapter(private val items: List<ROSOrderItemTotal>) :
    RecyclerView.Adapter<PlacedItemsTotalAdapter.ViewHolder>() {


    class ViewHolder(val binding: PlacedItemTotalBinding) : RecyclerView.ViewHolder(binding.root) {
        fun onBind(item: ROSOrderItemTotal?) {
            binding.txtQty.text = item?.itemCount.toString()
            binding.txtItemName.text = item?.itemName.toString()


        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            PlacedItemTotalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlacedItemsTotalAdapter.ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.onBind(item)
    }

    override fun getItemCount(): Int = items.size


}