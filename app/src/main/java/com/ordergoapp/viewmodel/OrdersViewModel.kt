package com.ordergoapp.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.ordergoapp.service.data.*
import com.ordergoapp.service.data.pojoModel.AppVersion
import com.ordergoapp.service.data.pojoModel.Restaurant
import com.ordergoapp.service.datasource.FirebaseDataSource
import com.ordergoapp.service.datasource.Resource
import com.ordergoapp.service.local.SessionManager
import com.ordergoapp.service.repositry.ROSRepositry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Exception
import java.util.HashMap

class OrdersViewModel() : ViewModel() {
    private lateinit var repositry: ROSRepositry;
    var sessionManager : SessionManager? = null

    companion object {
        var placedOrdersTrueSnapshot = MutableLiveData<QuerySnapshot>()
        var placedOrdersFalseSnapshot = MutableLiveData<QuerySnapshot>()



        var placedOrders = MutableLiveData<List<OrderShape>>()
        var placedOrdersTrueTMA = MutableLiveData<List<OrderShape>>()
        var placedOrdersFalseTMA = MutableLiveData<List<OrderShape>>()

        var placedLiveTablesNumber = MutableLiveData<List<Int>>()
        var tableOrders = MutableLiveData<List<OrderShape>>()
        var completedOrders = MutableLiveData<List<OrderShape>>()
        var placedItems =  MutableLiveData<Resource<List<ROSOrderItem>>>()
        var restaurantInfo = MutableLiveData<Restaurant>()
        var versionInfo = MutableLiveData<AppVersion>()
        //var sharedItemSessions = MutableLiveData<List<SharedItemSession>>()
    }


    fun assignRepositry(repositry: ROSRepositry) {
        this.repositry = repositry
    }

    fun getAllOrdersAsSnapShotfFalse(sessionManager: SessionManager) : MutableLiveData<QuerySnapshot>{
        this.sessionManager = sessionManager
        viewModelScope.launch {
            placedOrdersFalseSnapshot = repositry.getAllOrdersAsSnapShotFalse()
        }
        return placedOrdersFalseSnapshot
    }
    fun getAllOrdersAsSnapShotTrue(sessionManager: SessionManager) : MutableLiveData<QuerySnapshot>{
        this.sessionManager = sessionManager
        viewModelScope.launch {
            placedOrdersTrueSnapshot = repositry.getAllOrdersAsSnapShotTrue()
        }
        return placedOrdersTrueSnapshot
    }
    fun setAllDataToRetrieved() {
        placedOrdersFalseTMA.value = snp(placedOrdersFalseSnapshot.value, false)
        placedOrdersTrueTMA.value = snp(placedOrdersTrueSnapshot.value, false)
        placedItems.value = Resource.success(snapItemsList(placedOrdersFalseSnapshot.value).plus(snapItemsList(placedOrdersTrueSnapshot.value)))
        placedOrders.value = snp(placedOrdersFalseSnapshot.value, false).plus(snp(placedOrdersTrueSnapshot.value, false))
        completedOrders.value = snp(placedOrdersFalseSnapshot.value, true).plus(snp(placedOrdersTrueSnapshot.value, true))

    }




    fun listenToOrdersTrueTMA():  MutableLiveData<QuerySnapshot> {
        viewModelScope.launch {
            placedOrdersTrueSnapshot.value?.plus(repositry.listenToOrdersTrueTMA())
        }
        return placedOrdersTrueSnapshot
    }

    fun listenToOrdersFalseTMA():  MutableLiveData<QuerySnapshot> {
        viewModelScope.launch {
            placedOrdersFalseSnapshot.value?.plus(repositry.listenToOrdersFalseTMA())
        }
        return placedOrdersFalseSnapshot
    }


    fun getAllPlacedOrders(isCompeleted: Boolean): MutableLiveData<List<OrderShape>> {
        return if (isCompeleted) completedOrders else placedOrders
    }

    fun getTableOrders(tableId: Int): MutableLiveData<List<OrderShape>> {
        tableOrders = repositry.getTableOrders(tableId)
        return tableOrders
    }
    fun updateOrderItemStatusFunctions(
        orderId: String,
        rosType: String,
        uid: String,
        status: String
    ) {
        viewModelScope.launch {
            repositry.updateItemStatusFunction(orderId, rosType, uid, status)
        }
    }

    fun updateItem(orderId: String, rosType: String, itemId: String, status: String) {
        repositry.updateItemStatus(orderId, rosType, itemId, status)
    }

    fun updateSharedItem(sessionId: String, itemId: String, status: String) {
        repositry.updateSharedItemStatus(sessionId, itemId, status)
    }


    fun getRestaurantInfo(): MutableLiveData<Restaurant> {
        return try {
            restaurantInfo = repositry.getRestaurantInfo()!!
            return restaurantInfo
        } catch (e: Exception) {
            restaurantInfo
        }
    }

    fun getCurrentAppVersion(): MutableLiveData<AppVersion> {
        return try {
            versionInfo = repositry.getCurrentROSVersion()
            versionInfo
        } catch (e: Exception) {
            versionInfo
        }
    }

    fun setPrinted(orderId: String?) {
        orderId?.let { repositry.setPrintedDate(it) }
    }


    fun snp(snapshots: QuerySnapshot?, isCompletedOrders: Boolean): ArrayList<OrderShape> {

        var orderShapeList = arrayListOf<OrderShape>()
        var isFirst = true
        try {
            orderShapeList =
                snapshots?.documents?.mapNotNull {
                    val orderShape = it.toObject(OrderShape::class.java)
                    orderShape?.orderId = it.reference.id

                    val ros = sessionManager?.fetchROS()
                    val restId: String = sessionManager?.fetchRESID()!!
                    val list = arrayListOf<ROSOrderItem>()
                    val itemsList = arrayListOf<String>()
                    var isTotalCompleted = true;
                    var hasCompletedItems = false;
                    val gson = Gson()
                    var jsonElement: JsonElement
                    //Log.e("Order snap", orderShape.toString())

                    if (sessionManager?.fetchROSType().equals("t")) {
                        for (rosType in orderShape?.ROSTypes!!) {
                            val map = it[rosType!!] as HashMap<String, Object>?
                            itemsList.addAll(map?.get("items") as List<String>)
                            if (map != null) {
                                for (node in map) {
                                    if (node.key != "items" && node.key != "sharedItems") {
                                        jsonElement = gson.toJsonTree(node.value)
                                        val orderItem: ROSOrderItem =
                                            gson.fromJson(
                                                jsonElement,
                                                ROSOrderItem::class.java
                                            )
                                        orderItem.ros = rosType
                                        orderItem.restId = restId
                                        orderItem.orderId = orderShape.orderId
                                        if (isCompletedOrders) {
                                            if (!orderItem.status?.completed.isNullOrEmpty()
                                                && orderItem.status?.delivered.isNullOrEmpty()
                                            ) {
                                                list.add(orderItem)
                                                hasCompletedItems = true;
                                            } else
                                                isTotalCompleted = false
                                        } else {
                                            if (orderItem.status?.completed.isNullOrEmpty()) {
                                                isTotalCompleted = false
                                                list.add(orderItem)
                                            }
                                        }
                                    }
                                }

                            }
                        }
                    }
                    else {
                        //Log.e("Order ", "not T on B")
                        val map = it[ros!!] as HashMap<String, Object>?
                        //Log.e("Order ", "map: ${map}")
                        itemsList.addAll(map?.get("items") as List<String>)
                        //Log.e("Order ", "itemList: ${itemsList}")

                        if (map != null) {
                            for (node in map) {
                                // Log.e("Order ", "node: ${node}")//item{}
                                //Log.e("Order ", "nodeKeyB: ${node.key}") //item_1_0_1636372758266
                                if (node.key != "items" && node.key != "sharedItems") {
                                    //  Log.e("Order ", "nodeKeyA: ${node.key}")
                                    jsonElement = gson.toJsonTree(node.value) //{}
                                    val orderItem: ROSOrderItem =
                                        gson.fromJson(
                                            jsonElement,
                                            ROSOrderItem::class.java
                                        )

                                    orderItem.ros = ros
                                    orderItem.restId = restId
                                    orderItem.orderId = orderShape?.orderId
                                    //Log.e("Order ", "orderItem: ${orderItem.toString()}")

                                    if (isCompletedOrders) {
                                        if (!orderItem.status?.completed.isNullOrEmpty()
                                            && orderItem.status?.delivered.isNullOrEmpty()
                                        ) {
                                            list.add(orderItem)
                                            hasCompletedItems = true;
                                        } else
                                            isTotalCompleted = false
                                    } else {
                                        //Log.e("Order ", "orderItemStatus: ${orderItem.status?.completed}")
                                        if (orderItem.status?.completed.isNullOrEmpty()) {
                                            isTotalCompleted = false
                                            list.add(orderItem)
                                        }
                                    }

                                    // Log.e("Order ", "orderItemFtrCmpt: ${list.toString()}")
                                }

                            }

                        }

                        if (orderShape?.sharedItems != null) {
                            for (orderItem in orderShape?.sharedItems!!) {
                                if (orderItem.ROSType == ros && orderItem?.csm == orderShape?.order?.csm) {
                                    orderItem.ros = ros;
                                    orderItem.restId = restId
                                    orderItem.orderId = orderShape?.orderId
                                    orderItem.is_shared = true
                                    if (isCompletedOrders) {
                                        if (!orderItem.status?.completed.isNullOrEmpty()
                                            && orderItem.status?.delivered.isNullOrEmpty()
                                        ) {
                                            list.add(orderItem)
                                            hasCompletedItems = true;
                                        } else
                                            isTotalCompleted = false
                                    } else {
                                        if (orderItem.status?.completed.isNullOrEmpty()) {
                                            isTotalCompleted = false
                                            list.add(orderItem)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    orderShape?.rosOrder = RosOrder()
                    orderShape?.rosOrder?.items = itemsList
                    orderShape?.rosOrder?.itemsList = list
                    orderShape?.isTotalCompleted = isTotalCompleted

                    if (isCompletedOrders) {
                        if (isTotalCompleted || hasCompletedItems)
                            orderShape
                        else
                            null
                    } else {
                        if (!isTotalCompleted)
                            orderShape
                        else
                            null
                    }

                }?.toMutableList() as ArrayList<OrderShape>

            if (!isFirst) {
                var orderShapeChanges = snapshots?.documentChanges?.map {
                    var orderShape = it.document.toObject(OrderShape::class.java)
                    orderShape?.orderId = it.document.reference.id
                    when (it.type) {
                        DocumentChange.Type.ADDED -> orderShape.docStatus =
                            FirebaseDataSource.NEWDOC
                        DocumentChange.Type.MODIFIED -> orderShape.docStatus =
                            FirebaseDataSource.MODIFIEDDOC
                        DocumentChange.Type.REMOVED -> orderShape.docStatus =
                            FirebaseDataSource.DELETEDDOC
                    }
                    orderShape
                }

                orderShapeList = orderShapeList.map { orderShape ->
                    val temp: OrderShape? =
                        orderShapeChanges.find { it -> it?.orderId == orderShape.orderId };
                    if (temp != null)
                        orderShape.docStatus = temp.docStatus
                    orderShape
                } as ArrayList<OrderShape>

            }
            isFirst = false
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return orderShapeList;
    }

    fun snapItemsList(snapshots: QuerySnapshot?): List<ROSOrderItem> {
        val list = arrayListOf<ROSOrderItem>()

        try {
            list.clear()
            snapshots?.documents?.mapNotNull {
                val orderShape = it.toObject(OrderShape::class.java)

                val ros = sessionManager?.fetchROS()
                val gson = Gson()
                var jsonElement: JsonElement
                if (sessionManager?.fetchROSType().equals("t")) {
                    for (rosType in orderShape?.ROSTypes!!) {
                        val map = it[rosType!!] as HashMap<String, Object>?
                        if (map != null) {
                            for (node in map) {
                                if (node.key != "items" && node.key != "sharedItems") {
                                    jsonElement = gson.toJsonTree(node.value)
                                    val orderItem: ROSOrderItem =
                                        gson.fromJson(
                                            jsonElement,
                                            ROSOrderItem::class.java
                                        )
                                    orderItem.tableId = orderShape?.order?.tn
                                    orderItem.onId = orderShape?.order?.on
                                    orderItem.ros = rosType
                                    if (orderItem.status?.completed.isNullOrEmpty() && orderItem.status?.preparing.isNullOrEmpty()) {
                                        list.add(orderItem)
                                    }
                                }


                            }


                        }

                    }
                } else {
                    val map = it[ros!!] as HashMap<String, Object>?
                    if (map != null) {
                        for (node in map) {
                            if (node.key != "items" && node.key != "sharedItems") {
                                jsonElement = gson.toJsonTree(node.value)
                                val orderItem: ROSOrderItem =
                                    gson.fromJson(
                                        jsonElement,
                                        ROSOrderItem::class.java
                                    )
                                orderItem.tableId = orderShape?.order?.tn
                                orderItem.onId = orderShape?.order?.on
                                orderItem.ros = ros
                                if (orderItem.status?.completed.isNullOrEmpty()
                                    && orderItem.status?.preparing.isNullOrEmpty()
                                ) {
                                    list.add(orderItem)
                                }
                            }

                        }
                        //add Shared Items
                        if (orderShape?.sharedItems != null) {
                            for (orderItem in orderShape?.sharedItems!!) {
                                if (orderItem.ROSType == ros && orderItem?.csm == orderShape?.order?.csm) {
                                    orderItem.tableId = orderShape?.order?.tn
                                    orderItem.onId = orderShape?.order?.on
                                    orderItem.ros = ros
                                    if (orderItem.status?.completed.isNullOrEmpty()
                                        && orderItem.status?.preparing.isNullOrEmpty()
                                    ) {
                                        list.add(orderItem)
                                    }
                                }
                            }
                        }

                    }
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return list
    }

}