package com.ordergoapp.ui.adapter

import android.animation.*
import android.content.Context
import android.graphics.Color
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.ddd.androidutils.DoubleClick
import com.ddd.androidutils.DoubleClickListener
import com.google.firebase.functions.FirebaseFunctions
import com.google.gson.Gson
import com.ordergoapp.R
import com.ordergoapp.databinding.CompletedOrderItemBinding
import com.ordergoapp.databinding.PlacedOrderItemBinding
import com.ordergoapp.service.data.IngItem
import com.ordergoapp.service.data.ROSOrderItem
import com.ordergoapp.utils.Utils
import java.text.SimpleDateFormat
import java.util.*


class OrderItemsAdapter(
    private val list: List<ROSOrderItem>?,
    val listner: OrdersAdapter.OnChildClickListener?, val type: Int
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    //private var compositeDisposable = CompositeDisposable()
    companion object {
        const val CompletedItem = 0
        const val PlacedItem = 1
        const val DetailedItem = 2
        const val TAG = "OrderRecyclerItem"

        lateinit var context: Context
    }


    class ViewHolder(val binding: PlacedOrderItemBinding) : RecyclerView.ViewHolder(binding.root) {
        var clicked = false;

        fun onBind(item: ROSOrderItem?) {
            binding.txtQty.text = item?.QTY.toString()
            binding.txtItemName.text = item?.name.toString()
            binding.txtItemServing.text = item?.serving?.capitalize().toString()
            item?.option.let {
                if (!item?.option.isNullOrEmpty())
                    binding.txtOption.text = it.toString()
            }

            if (!item?.notes.isNullOrEmpty()) {
                binding.txtNotes.text = "Note: ${item?.notes.toString()}"
                binding.txtNotes.visibility = View.VISIBLE
            } else {
                binding.txtNotes.visibility = View.VISIBLE
                binding.txtNotes.text = "Note : N/ِA"
            }


            if (item?.status?.preparing != null &&
                item?.status?.preparing!!.isNotEmpty()
            ) {
                binding.txtOrderStatus.text = "Preparing"
            } else if (item?.status?.placed != null &&
                item?.status?.placed!!.isNotEmpty()
            ) {
                binding.txtOrderStatus.text = "Placed"
            }

            if (item?.ing.isNullOrEmpty())
                return

            val arrayAdapter = object : ArrayAdapter<IngItem>(
                this.binding.listViewAddOns.context,
                android.R.layout.simple_list_item_1,
                item?.ing?.toMutableList()!!
            ) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getView(position, convertView, parent)
                    //(view as TextView).setTextSize(16F)
                    (view as TextView).setTextColor(
                        if (item?.status?.completed != null &&
                            item?.status?.completed!!.isNotEmpty()
                        ) {
                            Color.WHITE

                        } else {
                            if (item?.status?.preparing != null &&
                                item?.status?.preparing!!.isNotEmpty()
                            )
                                Color.BLACK else Color.WHITE
                        }
                    ) // here can be your logic
                    return view
                }
            }

            binding.listViewAddOns.apply {
                adapter = arrayAdapter

                ArrayAdapter<IngItem>(
                    this.context,
                    android.R.layout.simple_list_item_1, android.R.id.text1,
                    item?.ing?.toMutableList()!!
                )


            }


        }
    }

    class CompletedViewHolder(val binding: CompletedOrderItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun onBind(item: ROSOrderItem?) {
            binding.txtQty.text = item?.QTY.toString()
            binding.txtItemName.text = item?.name.toString()
            binding.txtItemServing.text = item?.serving?.capitalize().toString()
            item?.option.let {
                if (!item?.option.isNullOrEmpty())
                    binding.txtOption.text = it.toString()
            }

            if (!item?.notes.isNullOrEmpty()) {
                binding.txtNotes.text = "Note: ${item?.notes.toString()}"
                binding.txtNotes.visibility = View.VISIBLE
            } else {
                binding.txtNotes.visibility = View.VISIBLE
                binding.txtNotes.text = "Note : N/ِA"
            }

            if (item?.status?.completed != null &&
                item?.status?.completed!!.isNotEmpty()
            ) {
                binding.txtOrderStatus.text = "Completed"
            }


            val arrayAdapter = object : ArrayAdapter<IngItem>(
                this.binding.listViewAddOns.context,
                android.R.layout.simple_list_item_1,
                item?.ing?.toMutableList()!!
            ) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getView(position, convertView, parent)
                    (view as TextView).setTextColor(
                        if (item?.status?.completed != null &&
                            item?.status?.completed!!.isNotEmpty()
                        ) {
                            Color.WHITE

                        } else {
                            Color.WHITE
                        }
                    )
                    return view
                }
            }

            binding.listViewAddOns.apply {
                adapter = arrayAdapter

                ArrayAdapter<IngItem>(
                    this.context,
                    android.R.layout.simple_list_item_1, android.R.id.text1,
                    item?.ing?.toMutableList()!!
                )


            }

            try {
                if (!item?.status?.placed!!.isNullOrEmpty() &&
                    !item?.status?.completed!!.isNullOrEmpty()
                ) {
                    val diff = Utils.getDateTimeDiff(
                        item?.status?.completed.toString(),
                        item?.status?.placed.toString()
                    )
                    binding.timer.base = SystemClock.elapsedRealtime() - diff
                    //binding.timer.start()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("OrderDetails ", "Exception: $e")
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        context = parent.context
        return if (viewType == PlacedItem || viewType == DetailedItem) {
            val binding =
                PlacedOrderItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            ViewHolder(binding)
        } else {
            val binding =
                CompletedOrderItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            CompletedViewHolder(binding)
        }

    }

    override fun getItemCount(): Int {
        return list?.size ?: 0
    }

    private fun flipAction(holder: ViewHolder) {
        val oa1 = ObjectAnimator.ofFloat(holder.itemView, "scaleX", 1f, 0f)
        val oa2 = ObjectAnimator.ofFloat(holder.itemView, "scaleX", 0f, 1f)
        oa1.interpolator = DecelerateInterpolator()
        oa2.interpolator = AccelerateDecelerateInterpolator()
        oa1.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                oa2.start()
                changeViewColors(holder)
            }
        })
        oa1.start()
        oa1.duration = 50
        oa2.duration = 50
//        val scale: Float = binding.root.resources.displayMetrics.density
//        binding.root.cameraDistance = 8000 * scale
//        val animatorSet1 = AnimatorInflater.loadAnimator(
//            binding.root.context,
//            R.animator.out_animation
//        ) as AnimatorSet
//        val animatorSet2 = AnimatorInflater.loadAnimator(
//            binding.root.context,
//            R.animator.in_animation
//        ) as AnimatorSet
//        animatorSet1.setTarget(binding.root)
//        animatorSet2.setTarget(binding.root)
//        animatorSet1.addListener(object : AnimatorListenerAdapter() {
//            @RequiresApi(Build.VERSION_CODES.O)
//            override fun onAnimationEnd(animation: Animator) {
//                super.onAnimationEnd(animation)
//                animatorSet1.reverse()
//                changeViewColors(binding)
//            }
//
//        })
//        animatorSet1.start()
//        animatorSet2.start()

    }

    private fun changeViewColors(holder: ViewHolder) {

        //holder.binding.layoutContent.setBackgroundResource(R.drawable.itemcard_active_background)
        //holder.binding.txtNotes.setBackgroundResource(R.drawable.notes_rounded_corners_active)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = list?.get(position)
        try {
            if (holder is ViewHolder) {

                with(holder)
                {
                    //binding.txtQty.text = item?.QTY.toString()
                    holder.onBind(item)
                    Log.e("OrderDetails ", "bind")
                    holder.binding.root.alpha = 1F

                    try {
                        if (item?.status?.placed != null &&
                            item?.status?.placed!!.isNotEmpty()
                        ) {
                            val diff = Utils.getDateTimeDiff(
                                item?.status?.placed.toString()
                            )
                            binding.timer.base = SystemClock.elapsedRealtime() - diff
                            binding.timer.start()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Log.e("OrderDetails ", "Exception: $e")
                    }

                    if (holder.itemViewType == PlacedItem) {

                        if (item?.status?.preparing != null &&
                            item?.status?.preparing!!.isNotEmpty()
                        ) {
                            Log.e("OrderDetails ", "Details Al")
                            changeBgToPreparingStatus(binding)
                            holder.clicked = true
                        } else if (item?.status?.placed != null &&
                            item?.status?.placed!!.isNotEmpty()
                        ) {
                            Log.e("OrderDetails ", "Details Al")
                            binding.layoutContent.setBackgroundResource(R.drawable.itemcard_background)

                            binding.txtQty.setTextColor(Color.WHITE)
                            binding.txtItemName.setTextColor(Color.WHITE)
                            binding.seq1.setTextColor(
                                binding.seq1.resources.getColor(
                                    R.color.primary,
                                    null
                                )
                            )
                        }
                    } else if (holder.itemViewType == DetailedItem) {
                        Log.e("OrderDetails ", "Details Al")
                        /* val diff =
                             dateFormat.parse(item?.status?.completed).time - dateFormat.parse(
                                 item?.status?.placed
                             ).time
                         binding.timer.base = SystemClock.elapsedRealtime() - diff
                         binding.layoutContent.setBackgroundResource(R.drawable.itemcard_background)
                         binding.txtNotes.setBackgroundResource(R.drawable.notes_rounded_corners)
                         binding.txtQty.setTextColor(Color.BLACK)
                         binding.txtItemName.setTextColor(Color.BLACK)
                         binding.timer.setTextColor(Color.BLACK)
                         binding.seq1.setTextColor(
                             binding.seq1.resources.getColor(
                                 R.color.primary,
                                 null
                             )
                         )*/

                    }

                if (holder.itemViewType == PlacedItem) {
                    val doubleClick = DoubleClick(object : DoubleClickListener {

                        override fun onSingleClickEvent(view: View?) {
                        }

                        override fun onDoubleClickEvent(view: View?) {
                            Utils.showCustomLoadingDialog(context)

                            if (item?.status?.preparing.isNullOrEmpty()) {
                                //flipAction(holder)
                                /* holder.itemView.animate().scaleX(-1F).setDuration(300)
                                     .setStartDelay(50)
                                     .start();*/

                                item?.let {
                                    /*val map = hashMapOf(
                                        "orderId" to it.orderId,
                                        "restId" to it.restId,
                                        "ROSType" to it.ros,
                                        "itemUID" to it.uid,
                                        "statusType" to "preparing",
                                    )
                                    //map["statusType"] = "preparing"
                                    Log.e(TAG, "funAdapterResultPreparing: $map")*/
                                    //callChangeStatusFunction(map, binding, OrderStatus.PREPARING)

                                    listner?.onChildClicked(
                                        it.ros,
                                        it?.uid,
                                        it?.is_shared,
                                        it?.sessionRef
                                    )

                                    changeBgToPreparingStatus(binding)

                                    /*if (it.is_shared)
                                        listner?.onChildClicked(
                                            it.ros,
                                            it?.uid,
                                            it?.is_shared,
                                            it?.sessionRef
                                        )
                                    else
                                        listner?.onChildClicked(
                                            it.ros,
                                            it?.uid,
                                            it.is_shared,
                                            it?.sessionRef
                                        )*/
                                }
                            }
                            else if (item?.status?.preparing != null &&
                                item?.status?.preparing!!.isNotEmpty()
                            ) {
                                item?.let {
                                   /* val map = hashMapOf(
                                        "orderId" to it.orderId,
                                        "restId" to it.restId,
                                        "ROSType" to it.ros,
                                        "itemUID" to it.uid,
                                        "statusType" to "completed",
                                    )
                                    Log.e(TAG, "funAdapterResultPreparing: $map")
                                    //map["statusType"] = "completed"*/
                                    //callChangeStatusFunction(map, binding, OrderStatus.COMPLETED)

                                    listner?.onChildCompleted(
                                        it.ros,
                                        it?.uid!!,
                                        it?.is_shared,
                                        it?.sessionRef
                                    )

                                    /*if (it.is_shared)
                                        listner?.onChildCompleted(
                                            it.ros,
                                            it?.uid!!,
                                            it?.is_shared,
                                            it?.sessionRef
                                        )
                                    else
                                        listner?.onChildCompleted(
                                            it?.ros,
                                            it?.uid,
                                            it.is_shared,
                                            it?.sessionRef
                                        )*/
                                }

                                holder.itemView.alpha = 0.3F
                            } else
                                holder.itemView.alpha = 1F


                        }
                    })
                    holder.itemView.setOnClickListener(doubleClick)
                }

            }
            } else {
                (holder as CompletedViewHolder).onBind(item)
                val doubleClick = DoubleClick(object : DoubleClickListener {

                    override fun onSingleClickEvent(view: View?) {

                    }

                    override fun onDoubleClickEvent(view: View?) {
                        item?.let {
                            if (it.is_shared)
                                listner?.onChildClicked(
                                    it.ros,
                                    it?.uid!!,
                                    it?.is_shared,
                                    it?.sessionRef
                                )
                            else
                                listner?.onChildClicked(
                                    it.ros,
                                    it?.uid,
                                    it?.is_shared,
                                    it?.sessionRef
                                )
                        }

                    }
                })
                holder.itemView.setOnClickListener(doubleClick)
            }
        } catch (ex: Exception) {
            ex.printStackTrace();
        }
    }

    private fun changeBgToPreparingStatus(binding: PlacedOrderItemBinding) {
        binding.layoutContent.setBackgroundResource(R.drawable.itemcard_active_background)
        binding.txtQty.setTextColor(Color.BLACK)
        binding.seq1.setTextColor(Color.WHITE)
        binding.txtItemName.setTextColor(Color.BLACK)
    }

   /* private fun callChangeStatusFunction(map: HashMap<String, String?>,
                                         binding: PlacedOrderItemBinding, orderStatus : OrderStatus) {
        FirebaseFunctions.getInstance("europe-west2")
            .getHttpsCallable("UpdateOrderItemStatusCF")
            .call(map)
            .continueWith { task ->
                val result = task.result?.data
                Log.e(TAG, "funAdapterResult: $result")

                checkResultStatus(result, binding, orderStatus)

                result
            }
    }

    private fun checkResultStatus(result: Any?,
                                  binding: PlacedOrderItemBinding,
                                  orderStatus : OrderStatus) {
        val json = Gson().toJson(result)
        val resultBoolean = Gson().fromJson(json, Boolean::class.java)
        if (!resultBoolean) {
            Log.e(TAG, "funAdapterResultFailure:Result: $result")
            Toast.makeText(context, context.getString(R.string.message_unknown_error), Toast.LENGTH_LONG).show()
            Utils.hideCustomLoadingDialog()
        }else{
            when (orderStatus){
                OrderStatus.COMPLETED -> null
                OrderStatus.PREPARING -> {
                    changeBgToPreparingStatus(binding)
                }
            }
            //notifyDataSetChanged()
            Toast.makeText(context, context.getString(R.string.message_order_status_changed), Toast.LENGTH_LONG).show()
            Utils.hideCustomLoadingDialog()
        }
    }*/

    override fun getItemViewType(position: Int): Int {
        return type
    }

}