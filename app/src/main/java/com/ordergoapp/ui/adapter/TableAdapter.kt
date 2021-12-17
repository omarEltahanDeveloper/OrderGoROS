package com.ordergoapp.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ordergoapp.R
import com.ordergoapp.databinding.ItemTableBinding


class TableAdapter(
    private val allTables: List<Int>,
    private val liveTables: List<Int>,
    val context: Context,
    val parentListener: OnItemClickListener?
) : RecyclerView.Adapter<TableAdapter.ViewHolder>(){


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val binding =
            ItemTableBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return allTables.size
    }

    @SuppressLint("SimpleDateFormat", "StringFormatMatches")
    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val table = allTables.get(position)
        with(holder)
        {
            binding.tableNumber.text = table.toString()

            binding.root.setOnClickListener{
                parentListener?.onChildClicked(table, it)
            }

            if (liveTables.contains(table)) {
                binding.lytTable.setBackgroundResource(R.drawable.btn_bg_solid_orange)
                binding.tableIcon.setColorFilter(ContextCompat.getColor(context, R.color.primary_dark))
            }else{
                binding.lytTable.setBackgroundResource(R.drawable.btn_bg_solid_black)
                binding.tableIcon.setColorFilter(ContextCompat.getColor(context, R.color.primary))
            }
        }

    }


    inner class ViewHolder(val binding: ItemTableBinding) :
        RecyclerView.ViewHolder(binding.root){
    }

    interface OnItemClickListener {
        fun onChildClicked(
            tableId: Int,
            view : View
        )

    }

}