package com.ordergoapp.service.datasource

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.ordergoapp.service.local.SessionManager
import com.ordergoapp.service.data.*
import com.ordergoapp.service.data.pojoModel.AppVersion
import com.ordergoapp.service.data.pojoModel.Restaurant
import com.ordergoapp.ui.adapter.OrderItemsAdapter
import com.ordergoapp.ui.main.MainActivity
import com.ordergoapp.utils.Utils
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
class FirebaseDataSource(private val sessionManager: SessionManager) {
    private val db = Firebase.firestore

    companion object {
        final val NEWDOC = 1
        final val MODIFIEDDOC = 2
        final val DELETEDDOC = 3

        final val TAG = "FirebaseDataSource"
    }

    fun getRestaurantInfo(resID: String): MutableLiveData<Restaurant> {
        val docRef = db.collection("Restaurants").document(resID)
        val mutableLiveData = MutableLiveData<Restaurant>()
        docRef.addSnapshotListener { document, e ->
            if (e != null) {
                Log.w(TAG, "listen:error", e)
                return@addSnapshotListener
            }
            val restaurant = document?.toObject(Restaurant::class.java)

            if (restaurant != null) {
                restaurant.resID = document.id
                sessionManager.saveRESID(document.id)
                sessionManager.savePid(restaurant.pid)
                sessionManager.saveResNTable(restaurant.nTables)
                SessionManager.nTable = restaurant.nTables
                mutableLiveData.value = restaurant!!
            }
        }
        return mutableLiveData
    }

    fun getCurrentROSVersion(): MutableLiveData<AppVersion> {
        val docRef = db.collection("AppsVersion").document("ROS")
        val mutableLiveData = MutableLiveData<AppVersion>()
        docRef.addSnapshotListener { document, e ->
            if (e != null) {
                Log.w(TAG, "listen:error", e)
                return@addSnapshotListener
            }
            val appVersion = document?.toObject(AppVersion::class.java)
            mutableLiveData.value = appVersion!!

        }
        return mutableLiveData
    }

    fun getAllROS(resID: String): MutableLiveData<List<ROS>> {
        val mutableLiveData = MutableLiveData<List<ROS>>()
        db.collection("Restaurants/${resID}/ROS")
            .whereEqualTo("status", false)
            .addSnapshotListener { documents, e ->
                if (e != null) {
                    Log.w(TAG, "listen:error", e)
                    return@addSnapshotListener
                }
                val rosList = arrayListOf<ROS>()
                var ros: ROS
                if (documents != null) {
                    for (doc in documents) {
                        ros = doc.toObject(ROS::class.java)
                        ros.id = doc.id
                        rosList.add(ros)
                    }
                }

                mutableLiveData.value = rosList
            }
        return mutableLiveData
//            .get()
//            .addOnSuccessListener { documents ->
//                val rosList = documents.toObjects(ROS::class.java)
//                it.onSuccess(rosList)
//            }
//            .addOnFailureListener { exception ->
//                Log.w(TAG, "Error getting documents: ", exception)
//                it.onError(exception)
//            }

    }

    fun updateROSStatus(resID: String, rosId: String) {
        val itemReference = db.collection("Restaurants/${resID}/ROS").document(rosId)
        itemReference
            .update(
                mapOf(
                    "status"
                            to true
                )
            )

    }

    fun getAllOrders2(
        isCompletedOrders: Boolean,
        isTMA: Boolean
    ): MutableLiveData<List<OrderShape>> {
        val mutableLiveData = MutableLiveData<List<OrderShape>>()
        var orderShapeList = arrayListOf<OrderShape>()
        var isFirst = true;
        try {
            //Check if order payInAdvance
            when (isTMA) {
                false ->
                    if (sessionManager.fetchROSType().equals("t")) {
                        db.collection("Orders")
                            .whereEqualTo("order.restId", sessionManager.fetchRESID())
                            .whereEqualTo("canceled", "")
                            .whereNotEqualTo("paid", "")
                            .whereEqualTo("isDelivered", false)
                            //.whereEqualTo("order.tn", 0)
                            .orderBy("paid", Query.Direction.ASCENDING)
                            .orderBy("dateTime", Query.Direction.ASCENDING)
                    } else {
                        db.collection("Orders")
                            .whereArrayContains(
                                "ROSTypes",
                                sessionManager.fetchROS()!!
                            )
                            .whereEqualTo("order.restId", sessionManager.fetchRESID())
                            .whereEqualTo("canceled", "")
                            .whereNotEqualTo("paid", "")
                            .whereEqualTo("isDelivered", false)
                            .orderBy("paid", Query.Direction.ASCENDING)
                            .orderBy("dateTime", Query.Direction.ASCENDING)
                    }
                true ->
                    if (sessionManager.fetchROSType().equals("t")) {
                        db.collection("Orders")
                            .whereEqualTo("order.restId", sessionManager.fetchRESID())
                            .whereEqualTo("canceled", "")
                            .whereEqualTo("isDelivered", false)
                            .whereEqualTo("paid", "")
                            .whereEqualTo("isTMA", true)
                            //.whereEqualTo("order.tn", 0)
                            .orderBy("dateTime", Query.Direction.ASCENDING)
                    } else {
                        db.collection("Orders")
                            .whereArrayContains(
                                "ROSTypes",
                                sessionManager.fetchROS()!!
                            )
                            .whereEqualTo("order.restId", sessionManager.fetchRESID())
                            .whereEqualTo("canceled", "")
                            .whereEqualTo("isDelivered", false)
                            .whereEqualTo("paid", "")
                            .whereEqualTo("isTMA", true)
                            .orderBy("dateTime", Query.Direction.ASCENDING)
                    }
            }
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        Log.w(TAG, "listen:error", e)
                        return@addSnapshotListener
                    }
                    try {
                        orderShapeList =
                            snapshots?.documents?.mapNotNull {
                                val orderShape = it.toObject(OrderShape::class.java)
                                ////   if (orderShape?.order?.tn != 0L ||sessionManager.fetchROS().equals("t_1")) {
                                orderShape?.orderId = it.reference.id
                                // if (orderShape?.order?.tn!! > 0L ||
                                //    (orderShape?.order?.tn == 0L && !orderShape?.paid?.isNullOrEmpty())
                                // ) {
                                //// orderShape?.rosOrders = ArrayList();
                                val ros = sessionManager.fetchROS()
                                val list = arrayListOf<ROSOrderItem>()
                                val itemsList = arrayListOf<String>()
                                var isTotalCompleted = true;
                                var hasCompletedItems = false;
                                val gson = Gson()
                                var jsonElement: JsonElement

                                if (sessionManager.fetchROSType().equals("t")) {

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
                                } else {
                                    val map = it[ros!!] as HashMap<String, Object>?
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
                                                orderItem.ros = ros
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
                                    if (orderShape?.sharedItems != null) {
                                        for (orderItem in orderShape?.sharedItems!!) {
                                            if (orderItem.ROSType == ros && orderItem?.csm == orderShape?.order?.csm) {
                                                orderItem.ros = ros;
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
                                // }
                                //else
                                // null


                            }?.toMutableList() as ArrayList<OrderShape>

                        if (!isFirst) {
                            var orderShapeChanges = snapshots?.documentChanges?.map {
                                var orderShape = it.document.toObject(OrderShape::class.java)
                                orderShape?.orderId = it.document.reference.id
                                when (it.type) {
                                    DocumentChange.Type.ADDED -> orderShape.docStatus = NEWDOC
                                    DocumentChange.Type.MODIFIED -> orderShape.docStatus =
                                        MODIFIEDDOC
                                    DocumentChange.Type.REMOVED -> orderShape.docStatus =
                                        DELETEDDOC
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

                    orderShapeList?.map {
                        Log.d("Order", it.toString())
                    }
                    mutableLiveData.value = orderShapeList
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return mutableLiveData
    }


    fun getAllOrders4(
        isCompletedOrders: Boolean
    ): MutableLiveData<List<OrderShape>> {
        val mutableLiveData = MutableLiveData<List<OrderShape>>()
        var orderShapeList = arrayListOf<OrderShape>()

        trueTMAListCondition().addSnapshotListener { snapshots, e ->
            Log.e("Orders snap1", "${snapshots?.documents?.size}")
            Log.e("Orders snap1", "${snapshots?.documents.toString()}")
            orderShapeList.addAll(snapshots?.documents?.mapNotNull {
                val orderShape = it.toObject(OrderShape::class.java)
                orderShape?.orderId = it.reference.id
                val ros = sessionManager.fetchROS()
                val list = arrayListOf<ROSOrderItem>()
                val itemsList = arrayListOf<String>()
                var isTotalCompleted = true;
                var hasCompletedItems = false;
                val gson = Gson()
                var jsonElement: JsonElement

                if (sessionManager.fetchROSType().equals("t")) {

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
                } else {
                    val map = it[ros!!] as HashMap<String, Object>?
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
                                orderItem.ros = ros
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
                    if (orderShape?.sharedItems != null) {
                        for (orderItem in orderShape?.sharedItems!!) {
                            if (orderItem.ROSType == ros && orderItem?.csm == orderShape?.order?.csm) {
                                orderItem.ros = ros;
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


            }?.toList() ?: ArrayList())

            falseTMAListCondition().addSnapshotListener { snap, e ->
                Log.e("Orders snap2", "${snap?.documents?.size}")
                Log.e("Orders snap2", "${snap?.documents.toString()}")
                orderShapeList.addAll(snap?.documents?.mapNotNull {
                    val orderShape = it.toObject(OrderShape::class.java)
                    ////   if (orderShape?.order?.tn != 0L ||sessionManager.fetchROS().equals("t_1")) {
                    orderShape?.orderId = it.reference.id
                    // if (orderShape?.order?.tn!! > 0L ||
                    //    (orderShape?.order?.tn == 0L && !orderShape?.paid?.isNullOrEmpty())
                    // ) {
                    //// orderShape?.rosOrders = ArrayList();
                    val ros = sessionManager.fetchROS()
                    val list = arrayListOf<ROSOrderItem>()
                    val itemsList = arrayListOf<String>()
                    var isTotalCompleted = true;
                    var hasCompletedItems = false;
                    val gson = Gson()
                    var jsonElement: JsonElement

                    if (sessionManager.fetchROSType().equals("t")) {

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
                    } else {
                        val map = it[ros!!] as HashMap<String, Object>?
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
                                    orderItem.ros = ros
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
                        if (orderShape?.sharedItems != null) {
                            for (orderItem in orderShape?.sharedItems!!) {
                                if (orderItem.ROSType == ros && orderItem?.csm == orderShape?.order?.csm) {
                                    orderItem.ros = ros;
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
                    // }
                    //else
                    // null


                }?.toList() ?: ArrayList())
            }
            mutableLiveData.value = orderShapeList
        }
        return mutableLiveData;
    }

    suspend fun getAllOrders3(
        isCompletedOrders: Boolean
    ): MutableLiveData<List<OrderShape>> {
        val mutableLiveData = MutableLiveData<List<OrderShape>>()
        var orderShapeList = arrayListOf<OrderShape>()

        falseTMAListCondition().addSnapshotListener { snapshotsOne, e ->
            if (e != null) {
                Log.w(TAG, "listen:error", e)
                return@addSnapshotListener
            }

            Log.e("Order snap", "falseTMA Size ${snapshotsOne?.documents?.size}")

            trueTMAListCondition().addSnapshotListener { snapshotsTwo, e ->
                if (e != null) {
                    Log.w(TAG, "listen:error", e)
                    return@addSnapshotListener
                }

                Log.e("Order snap", "trueTMA Size ${snapshotsTwo?.documents?.size}")

                if (orderShapeList?.isEmpty()) {
                    orderShapeList.addAll(snp(snapshotsOne, isCompletedOrders))
                } else {
                    orderShapeList.clear()
                    orderShapeList.addAll(snp(snapshotsOne, isCompletedOrders))
                }

                orderShapeList.addAll(snp(snapshotsTwo, isCompletedOrders))

                orderShapeList?.map {
                    Log.e("Order Map", it.toString())
                }
                mutableLiveData.value = orderShapeList
            }
        }
        return mutableLiveData;
    }

    suspend fun getAllOrdersFalseTMA(
        isCompletedOrders: Boolean
    ): MutableLiveData<List<OrderShape>> {
        val mutableLiveData = MutableLiveData<List<OrderShape>>()

        var orderShapeList = arrayListOf<OrderShape>()

        falseTMAListCondition().addSnapshotListener { snapshotsOne, e ->
            if (e != null) {
                Log.w(TAG, "listen:error", e)
                return@addSnapshotListener
            }

            Log.e("Order snap", "falseTMA Size ${snapshotsOne?.documents?.size}")

      /*      if (orderShapeList?.isEmpty()) {
                orderShapeList.addAll(snp(snapshotsOne, isCompletedOrders))
            } else {
                orderShapeList.clear()
                orderShapeList.addAll(snp(snapshotsOne, isCompletedOrders))
            }*/

            orderShapeList = snp(snapshotsOne, isCompletedOrders)

            orderShapeList?.map {
                Log.e("OrderFalseTMA Map", it.toString())
            }

            mutableLiveData.value = orderShapeList
        }

        return mutableLiveData;
    }

     suspend fun getAllOrdersTrueTMA(
        isCompletedOrders: Boolean
    ): MutableLiveData<List<OrderShape>> {
        val mutableLiveData = MutableLiveData<List<OrderShape>>()

        var orderShapeList = arrayListOf<OrderShape>()

        trueTMAListCondition().addSnapshotListener { snapshotsTwo, e ->
            if (e != null) {
                Log.w(TAG, "listen:error", e)
                return@addSnapshotListener
            }

            Log.e("Order snap", "trueTMA Size ${snapshotsTwo?.documents?.size}")

            /*if (orderShapeList?.isEmpty()) {
                orderShapeList.addAll(snp(snapshotsTwo, isCompletedOrders))
            } else {
                orderShapeList.clear()
                orderShapeList.addAll(snp(snapshotsTwo, isCompletedOrders))
            }*/

            orderShapeList = snp(snapshotsTwo, isCompletedOrders);

            //orderShapeList.addAll(snp(snapshotsTwo, isCompletedOrders))

            orderShapeList?.map {
                Log.e("OrderTrueTMA Map", it.toString())
            }
            mutableLiveData.value = orderShapeList
        }

        return mutableLiveData;
    }

    private fun snp(snapshots: QuerySnapshot?, isCompletedOrders: Boolean): ArrayList<OrderShape> {
        var orderShapeList = arrayListOf<OrderShape>()
        var isFirst = true

        Log.e("Order snap", "Data ${snapshots?.documents.toString()}")

        try {
            orderShapeList =
                snapshots?.documents?.mapNotNull {
                    val orderShape = it.toObject(OrderShape::class.java)
                    orderShape?.orderId = it.reference.id

                    val ros = sessionManager.fetchROS()
                    val restId: String = sessionManager.fetchRESID()!!
                    val list = arrayListOf<ROSOrderItem>()
                    val itemsList = arrayListOf<String>()
                    var isTotalCompleted = true;
                    var hasCompletedItems = false;
                    val gson = Gson()
                    var jsonElement: JsonElement
                    //Log.e("Order snap", orderShape.toString())

                    if (sessionManager.fetchROSType().equals("t")) {
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
                        DocumentChange.Type.ADDED -> orderShape.docStatus = NEWDOC
                        DocumentChange.Type.MODIFIED -> orderShape.docStatus = MODIFIEDDOC
                        DocumentChange.Type.REMOVED -> orderShape.docStatus = DELETEDDOC
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

    private fun falseTMAListCondition(): Query {
        return if (sessionManager.fetchROSType().equals("t")) {
            db.collection("Orders")
                .whereEqualTo("order.restId", sessionManager.fetchRESID())
                .whereEqualTo("canceled", "")
                .whereNotEqualTo("paid", "")
                .whereEqualTo("isDelivered", false)
                .whereEqualTo("order.tn", 0)
                .orderBy("paid", Query.Direction.ASCENDING)
                .orderBy("dateTime", Query.Direction.ASCENDING)
        } else {
            db.collection("Orders")
                .whereArrayContains(
                    "ROSTypes",
                    sessionManager.fetchROS()!!
                )
                .whereEqualTo("order.restId", sessionManager.fetchRESID())
                .whereEqualTo("canceled", "")
                .whereEqualTo("isDelivered", false)
                .whereNotEqualTo("paid", "")
                .orderBy("paid", Query.Direction.ASCENDING)
                .orderBy("dateTime", Query.Direction.ASCENDING)
        }
    }

    private fun trueTMAListCondition(): Query {
        return if (sessionManager.fetchROSType().equals("t")) {
            db.collection("Orders")
                .whereEqualTo("order.restId", sessionManager.fetchRESID())
                .whereEqualTo("canceled", "")
                .whereEqualTo("isDelivered", false)
                .whereEqualTo("paid", "")
                .whereEqualTo("isTMA", true)
                .whereEqualTo("order.tn", 0)
                .orderBy("dateTime", Query.Direction.ASCENDING)
        } else {
            db.collection("Orders")
                .whereArrayContains(
                    "ROSTypes",
                    sessionManager.fetchROS()!!
                )
                .whereEqualTo("order.restId", sessionManager.fetchRESID())
                .whereEqualTo("canceled", "")
                .whereEqualTo("isDelivered", false)
                .whereEqualTo("paid", "")
                .whereEqualTo("isTMA", true)
                //.orderBy("paid", Query.Direction.ASCENDING)
                .orderBy("dateTime", Query.Direction.ASCENDING)
        }
    }

    fun getAllOrders(isCompletedOrders: Boolean): MutableLiveData<List<OrderShape>> {
        val mutableLiveData = MutableLiveData<List<OrderShape>>()
        var orderShapeList = arrayListOf<OrderShape>()
        var isFirst = true;
        try {
            //Check if order payInAdvance
            when (sessionManager.fetchPid()) {
                true -> if (sessionManager.fetchROS().equals("t_1")) {
                    db.collection("Orders")
                        .whereEqualTo("order.restId", sessionManager.fetchRESID())
                        .whereEqualTo("canceled", "")
                        .whereNotEqualTo("paid", "")
                        .whereEqualTo("isDelivered", false)
                        //.whereEqualTo("isTMA", true)
                        .whereEqualTo("order.tn", 0)
                        .orderBy("paid", Query.Direction.ASCENDING)
                        .orderBy("dateTime", Query.Direction.ASCENDING)
                } else {
                    db.collection("Orders")
                        .whereArrayContains(
                            "ROSTypes",
                            sessionManager.fetchROS()!!
                        )
                        .whereEqualTo("order.restId", sessionManager.fetchRESID())
                        .whereEqualTo("canceled", "")
                        .whereNotEqualTo("paid", "")
                        .whereEqualTo("isDelivered", false)
                        .orderBy("paid", Query.Direction.ASCENDING)
                        .orderBy("dateTime", Query.Direction.ASCENDING)
                }
                false ->
                    if (sessionManager.fetchROS().equals("t_1")) {
                        db.collection("Orders")
                            .whereEqualTo("canceled", "")
                            .whereEqualTo("isDelivered", false)
                            .whereEqualTo("order.tn", 0)
                            .whereEqualTo("order.restId", sessionManager.fetchRESID())
                            .orderBy("dateTime", Query.Direction.ASCENDING)
                    } else {
                        db.collection("Orders")
                            .whereArrayContains(
                                "ROSTypes",
                                sessionManager.fetchROS()!!
                            )
                            .whereEqualTo("canceled", "")
                            .whereEqualTo("isDelivered", false)
                            .whereEqualTo("order.restId", sessionManager.fetchRESID())
                            .orderBy("dateTime", Query.Direction.ASCENDING)
                    }
            }
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        Log.w(TAG, "listen:error", e)
                        return@addSnapshotListener
                    }
                    try {
                        orderShapeList =
                            snapshots?.documents?.mapNotNull {
                                val orderShape = it.toObject(OrderShape::class.java)
                                //   if (orderShape?.order?.tn != 0L ||sessionManager.fetchROS().equals("t_1")) {
                                orderShape?.orderId = it.reference.id
                                if (orderShape?.order?.tn!! > 0L ||
                                    (orderShape?.order?.tn == 0L && !orderShape?.paid?.isNullOrEmpty())
                                ) {
                                    // orderShape?.rosOrders = ArrayList();
                                    val ros = sessionManager.fetchROS()
                                    val list = arrayListOf<ROSOrderItem>()
                                    val itemsList = arrayListOf<String>()
                                    var isTotalCompleted = true;
                                    var hasCompletedItems = false;
                                    val gson = Gson()
                                    var jsonElement: JsonElement

                                    if (sessionManager.fetchROS().equals("t_1")) {

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
                                    } else {
                                        val map = it[ros!!] as HashMap<String, Object>?
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
                                                    orderItem.ros = ros
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
                                        if (orderShape?.sharedItems != null) {
                                            for (orderItem in orderShape?.sharedItems!!) {
                                                if (orderItem.ROSType == ros && orderItem?.csm == orderShape?.order?.csm) {
                                                    orderItem.ros = ros;
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
                                } else
                                    null
                                //  }
//                            else
//                                null

                            }?.toMutableList() as ArrayList<OrderShape>

                        if (!isFirst) {
                            var orderShapeChanges = snapshots?.documentChanges?.map {
                                var orderShape = it.document.toObject(OrderShape::class.java)
                                orderShape?.orderId = it.document.reference.id
                                when (it.type) {
                                    DocumentChange.Type.ADDED -> orderShape.docStatus = NEWDOC
                                    DocumentChange.Type.MODIFIED -> orderShape.docStatus =
                                        MODIFIEDDOC
                                    DocumentChange.Type.REMOVED -> orderShape.docStatus =
                                        DELETEDDOC
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

                    orderShapeList?.map {
                        Log.d("Order", it.toString())
                    }
                    mutableLiveData.value = orderShapeList
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return mutableLiveData
    }

    fun getAllOrdersByTable2(
        isCompletedOrders: Boolean,
        tableId: Int
    ): MutableLiveData<List<OrderShape>> {
        val mutableLiveData = MutableLiveData<List<OrderShape>>()
        var orderShapeList = arrayListOf<OrderShape>()

        falseTMAListConditionByTable(tableId).addSnapshotListener { snapshotsOne, e ->
            if (e != null) {
                Log.w(TAG, "listen:error", e)
                return@addSnapshotListener
            }

            trueTMAListConditionByTable(tableId).addSnapshotListener { snapshotsTwo, e ->
                if (e != null) {
                    Log.w(TAG, "listen:error", e)
                    return@addSnapshotListener
                }

                if (orderShapeList?.isEmpty()) {
                    orderShapeList.addAll(snp(snapshotsOne, isCompletedOrders))
                } else {
                    orderShapeList.clear()
                    orderShapeList.addAll(snp(snapshotsOne, isCompletedOrders))
                }

                orderShapeList.addAll(snp(snapshotsTwo, isCompletedOrders))

                orderShapeList?.map {
                    Log.e("Order Map", it.toString())
                }
                mutableLiveData.value = orderShapeList
            }
        }
        return mutableLiveData;
    }

    private fun falseTMAListConditionByTable(tableNum: Int): Query {
        return db.collection("Orders")
            .whereArrayContains(
                "ROSTypes",
                sessionManager.fetchROS()!!
            )
            .whereEqualTo("order.restId", sessionManager.fetchRESID())
            .whereEqualTo("canceled", "")
            .whereEqualTo("isDelivered", false)
            .whereNotEqualTo("paid", "")
            .whereEqualTo("order.tn", tableNum)
            .orderBy("paid", Query.Direction.ASCENDING)
            .orderBy("dateTime", Query.Direction.ASCENDING)

    }

    private fun trueTMAListConditionByTable(tableNum: Int): Query {
        return db.collection("Orders")
            .whereArrayContains(
                "ROSTypes",
                sessionManager.fetchROS()!!
            )
            .whereEqualTo("order.restId", sessionManager.fetchRESID())
            .whereEqualTo("canceled", "")
            .whereEqualTo("isDelivered", false)
            .whereEqualTo("paid", "")
            .whereEqualTo("order.tn", tableNum)
            .whereEqualTo("isTMA", true)
            //.orderBy("paid", Query.Direction.ASCENDING)
            .orderBy("dateTime", Query.Direction.ASCENDING)
    }

    fun getAllOrdersByTable(tableId: Int): MutableLiveData<List<OrderShape>> {
        val mutableLiveData = MutableLiveData<List<OrderShape>>()
        var orderShapeList = arrayListOf<OrderShape>()
        var isFirst = true;
        try {
            //Check if order payInAdvance
            when (sessionManager.fetchPid()) {
                true -> if (sessionManager.fetchROS().equals("t_1")) {
                    db.collection("Orders")
                        .whereEqualTo("canceled", "")
                        .whereEqualTo("isDelivered", false)
                        .whereEqualTo("isTMA", true)
                        .whereEqualTo("order.tn", tableId)
                        .whereEqualTo("order.restId", sessionManager.fetchRESID())
                        .orderBy("paid", Query.Direction.ASCENDING)
                        .orderBy("dateTime", Query.Direction.ASCENDING)
                } else {
                    db.collection("Orders")
                        .whereArrayContains(
                            "ROSTypes",
                            sessionManager.fetchROS()!!
                        )
                        .whereEqualTo("canceled", "")
                        .whereNotEqualTo("paid", "")
                        .whereEqualTo("isDelivered", false)
                        .whereEqualTo("order.restId", sessionManager.fetchRESID())
                        .whereEqualTo("order.tn", tableId)
                        .orderBy("paid", Query.Direction.ASCENDING)
                        .orderBy("dateTime", Query.Direction.ASCENDING)
                }
                false ->
                    if (sessionManager.fetchROS().equals("t_1")) {
                        db.collection("Orders")
                            .whereEqualTo("canceled", "")
                            .whereEqualTo("isDelivered", false)
                            .whereEqualTo("order.tn", tableId)
                            .whereEqualTo("order.restId", sessionManager.fetchRESID())
                            .orderBy("dateTime", Query.Direction.ASCENDING)
                    } else {
                        db.collection("Orders")
                            .whereArrayContains(
                                "ROSTypes",
                                sessionManager.fetchROS()!!
                            )
                            .whereEqualTo("canceled", "")
                            .whereEqualTo("isDelivered", false)
                            .whereEqualTo("order.tn", tableId)
                            .whereEqualTo("order.restId", sessionManager.fetchRESID())
                            .orderBy("dateTime", Query.Direction.ASCENDING)
                    }
            }
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        Log.w(TAG, "listen:error", e)
                        return@addSnapshotListener
                    }
                    try {
                        orderShapeList =
                            snapshots?.documents?.mapNotNull {
                                val orderShape = it.toObject(OrderShape::class.java)
                                orderShape?.orderId = it.reference.id
                                if (orderShape?.order?.tn!! == tableId.toLong()) {

                                    val ros = sessionManager.fetchROS()
                                    val list = arrayListOf<ROSOrderItem>()
                                    val itemsList = arrayListOf<String>()
                                    var isTotalCompleted = true;
                                    var hasCompletedItems = false;
                                    val gson = Gson()
                                    var jsonElement: JsonElement

                                    if (sessionManager.fetchROS().equals("t_1")) {

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

                                                        if (!orderItem.status?.completed.isNullOrEmpty()
                                                            && orderItem.status?.delivered.isNullOrEmpty()
                                                        ) {
                                                            hasCompletedItems = true;
                                                        } else
                                                            isTotalCompleted = false

                                                        list.add(orderItem)

                                                    }

                                                }

                                            }
                                        }
                                    } else {
                                        val map = it[ros!!] as HashMap<String, Object>?
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
                                                    orderItem.ros = ros
                                                    if (!orderItem.status?.completed.isNullOrEmpty()
                                                        && orderItem.status?.delivered.isNullOrEmpty()
                                                    ) {
                                                        hasCompletedItems = true;
                                                    } else
                                                        isTotalCompleted = false

                                                    list.add(orderItem)


                                                }

                                            }

                                        }
                                        if (orderShape?.sharedItems != null) {
                                            for (orderItem in orderShape?.sharedItems!!) {
                                                if (orderItem.ROSType == ros && orderItem?.csm == orderShape?.order?.csm) {
                                                    orderItem.ros = ros;
                                                    orderItem.is_shared = true

                                                    if (!orderItem.status?.completed.isNullOrEmpty()
                                                        && orderItem.status?.delivered.isNullOrEmpty()
                                                    ) {
                                                        hasCompletedItems = true;
                                                    } else
                                                        isTotalCompleted = false

                                                    list.add(orderItem)
                                                }
                                            }
                                        }

                                    }

                                    orderShape?.rosOrder = RosOrder()
                                    orderShape?.rosOrder?.items = itemsList
                                    orderShape?.rosOrder?.itemsList = list
                                    orderShape?.isTotalCompleted = isTotalCompleted

                                    orderShape

                                } else
                                    null

                            }?.toMutableList() as ArrayList<OrderShape>

                        if (!isFirst) {
                            var orderShapeChanges = snapshots?.documentChanges?.map {
                                var orderShape = it.document.toObject(OrderShape::class.java)
                                orderShape?.orderId = it.document.reference.id
                                when (it.type) {
                                    DocumentChange.Type.ADDED -> orderShape.docStatus = NEWDOC
                                    DocumentChange.Type.MODIFIED -> orderShape.docStatus =
                                        MODIFIEDDOC
                                    DocumentChange.Type.REMOVED -> orderShape.docStatus =
                                        DELETEDDOC
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

                    orderShapeList?.map {
                        Log.d("Order", it.toString())
                    }
                    mutableLiveData.value = orderShapeList
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return mutableLiveData
    }

    suspend fun getItems2(
        resID: String
    ): MutableLiveData<Resource<List<ROSOrderItem>>> {
        val mutableLiveData = MutableLiveData<Resource<List<ROSOrderItem>>>()
        val list = arrayListOf<ROSOrderItem>()

        falseTMAListCondition().addSnapshotListener { snapshotsOne, e ->
            if (e != null) {
                Log.w(TAG, "listen:error", e)
                mutableLiveData.value = Resource.error("listen:error $e", null)
                return@addSnapshotListener
            }
            Log.e(TAG, "falseTMAListConditionLoading")
            mutableLiveData.value = Resource.loading(null)

            trueTMAListCondition().addSnapshotListener { snapshotsTwo, e ->
                if (e != null) {
                    Log.w(TAG, "listen:error", e)
                    mutableLiveData.value = Resource.error("listen:error $e", null)
                    return@addSnapshotListener
                }
                Log.e(TAG, "trueTMAListConditionLoading")
                mutableLiveData.value = Resource.loading(null)

                if (list?.isEmpty()) {
                    list.addAll(snapItemsList(snapshotsOne))
                } else {
                    list.clear()
                    list.addAll(snapItemsList(snapshotsOne))
                }

                list.addAll(snapItemsList(snapshotsTwo))

                list?.map {
                    Log.e(TAG, "Order Items ${it.toString()}")
                }
                mutableLiveData.value = Resource.success(list)
            }
        }
        return mutableLiveData;
    }

    suspend fun getItemsTrueTMA(
        resID: String
    ): MutableLiveData<Resource<List<ROSOrderItem>>> {
        val mutableLiveData = MutableLiveData<Resource<List<ROSOrderItem>>>()
        val list = arrayListOf<ROSOrderItem>()

        falseTMAListCondition().addSnapshotListener { snapshotsOne, e ->
            if (e != null) {
                Log.w(TAG, "listen:error", e)
                mutableLiveData.value = Resource.error("listen:error $e", null)
                return@addSnapshotListener
            }
            Log.e(TAG, "falseTMAListConditionLoading")
            mutableLiveData.value = Resource.loading(null)

            trueTMAListCondition().addSnapshotListener { snapshotsTwo, e ->
                if (e != null) {
                    Log.w(TAG, "listen:error", e)
                    mutableLiveData.value = Resource.error("listen:error $e", null)
                    return@addSnapshotListener
                }
                Log.e(TAG, "trueTMAListConditionLoading")
                mutableLiveData.value = Resource.loading(null)

                if (list?.isEmpty()) {
                    list.addAll(snapItemsList(snapshotsOne))
                } else {
                    list.clear()
                    list.addAll(snapItemsList(snapshotsOne))
                }

                list.addAll(snapItemsList(snapshotsTwo))

                list?.map {
                    Log.e(TAG, "Order Items ${it.toString()}")
                }
                mutableLiveData.value = Resource.success(list)
            }
        }
        return mutableLiveData;
    }

    suspend fun getItemsFalseTMA(
        resID: String
    ): MutableLiveData<Resource<List<ROSOrderItem>>> {
        val mutableLiveData = MutableLiveData<Resource<List<ROSOrderItem>>>()
        val list = arrayListOf<ROSOrderItem>()

        falseTMAListCondition().addSnapshotListener { snapshotsOne, e ->
            if (e != null) {
                Log.w(TAG, "listen:error", e)
                mutableLiveData.value = Resource.error("listen:error $e", null)
                return@addSnapshotListener
            }
            Log.e(TAG, "falseTMAListConditionLoading")
            mutableLiveData.value = Resource.loading(null)

            trueTMAListCondition().addSnapshotListener { snapshotsTwo, e ->
                if (e != null) {
                    Log.w(TAG, "listen:error", e)
                    mutableLiveData.value = Resource.error("listen:error $e", null)
                    return@addSnapshotListener
                }
                Log.e(TAG, "trueTMAListConditionLoading")
                mutableLiveData.value = Resource.loading(null)

                if (list?.isEmpty()) {
                    list.addAll(snapItemsList(snapshotsOne))
                } else {
                    list.clear()
                    list.addAll(snapItemsList(snapshotsOne))
                }

                list.addAll(snapItemsList(snapshotsTwo))

                list?.map {
                    Log.e(TAG, "Order Items ${it.toString()}")
                }
                mutableLiveData.value = Resource.success(list)
            }
        }
        return mutableLiveData;
    }

    fun snapItemsList(snapshots: QuerySnapshot?): List<ROSOrderItem> {
        val list = arrayListOf<ROSOrderItem>()

        try {
            list.clear()
            snapshots?.documents?.mapNotNull {
                val orderShape = it.toObject(OrderShape::class.java)

                val ros = sessionManager.fetchROS()
                val gson = Gson()
                var jsonElement: JsonElement
                if (sessionManager.fetchROSType().equals("t")) {
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

    fun getItems(resID: String): MutableLiveData<List<ROSOrderItem>> {
        val mutableLiveData = MutableLiveData<List<ROSOrderItem>>()
        val list = arrayListOf<ROSOrderItem>()
        try {
            //Check if order payInAdvance
            when (sessionManager.fetchPid()) {
                true -> if (sessionManager.fetchROS().equals("t_1")) {
                    db.collection("Orders")
                        .whereEqualTo("canceled", "")
                        .whereEqualTo("isDelivered", false)
                        .whereEqualTo("order.tn", 0)
                        .whereEqualTo("order.restId", sessionManager.fetchRESID())
                        .orderBy("paid", Query.Direction.ASCENDING)
                        .orderBy("dateTime", Query.Direction.ASCENDING)
                } else {
                    db.collection("Orders")
                        .whereArrayContains(
                            "ROSTypes",
                            sessionManager.fetchROS()!!
                        )
                        .whereEqualTo("canceled", "")
                        .whereNotEqualTo("paid", "")
                        .whereEqualTo("isDelivered", false)
                        .whereEqualTo("order.restId", sessionManager.fetchRESID())
                        .orderBy("paid", Query.Direction.ASCENDING)
                        .orderBy("dateTime", Query.Direction.ASCENDING)
                }
                false -> if (sessionManager.fetchROS().equals("t_1")) {
                    db.collection("Orders")
                        .whereEqualTo("order.tn", 0)
                        .whereEqualTo("canceled", "")
                        .whereEqualTo("isDelivered", false)
                        .whereEqualTo("order.restId", sessionManager.fetchRESID())
                        .orderBy("dateTime", Query.Direction.ASCENDING)
                } else {
                    db.collection("Orders")
                        .whereArrayContains(
                            "ROSTypes",
                            sessionManager.fetchROS()!!
                        )
                        .whereEqualTo("canceled", "")
                        .whereEqualTo("isDelivered", false)
                        .whereEqualTo("order.restId", sessionManager.fetchRESID())
                        .orderBy("dateTime", Query.Direction.ASCENDING)

                }
            }
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        Log.w(TAG, "listen:error", e)
                        return@addSnapshotListener
                    }
                    try {
                        list.clear()
                        snapshots?.documents?.mapNotNull {
                            val orderShape = it.toObject(OrderShape::class.java)
                            //  if (orderShape?.order?.tn != 0L||sessionManager.fetchROS().equals("t_1")) {
                            if (orderShape?.order?.tn!! > 0L ||
                                (orderShape?.order?.tn == 0L && !orderShape?.paid?.isNullOrEmpty())
                            ) {
                                val ros = sessionManager.fetchROS()
                                val gson = Gson()
                                var jsonElement: JsonElement
                                if (sessionManager.fetchROSType().equals("t")) {

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
                                                    if (orderItem.status?.completed.isNullOrEmpty() && orderItem.status?.preparing.isNullOrEmpty()) {
                                                        list.add(orderItem)
                                                    }
                                                }
                                            }
                                        }

                                    }
                                }
                            }
                        }
                        //}

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    mutableLiveData.value = list
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return mutableLiveData
    }

    fun updateItemStatus(orderId: String, ros: String, itemId: String, status: String) {
        Log.e("Status:  ", "fs $status $itemId")

        //try {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        dateFormat.timeZone = TimeZone.getTimeZone("GMT");
        val itemReference = db.collection("Orders").document(orderId)
        itemReference
            .update(
                mapOf(
                    "${ros}.${itemId}.status.${status}"
                            to dateFormat.format(Date())
                )
            )

        //} catch (e: Exception) {
        //    e.printStackTrace()
        //    Log.e("Status ", "Date Exception: $e")
        //}

    }

    fun updateSharedItemStatus(sessionId: String, itemId: String, status: String) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.UK)
        dateFormat.timeZone = TimeZone.getTimeZone("GMT");

        val itemReference =
            db.collection("Restaurants/${sessionManager.fetchRESID()}/open_session")
                .document(sessionId)

        itemReference
            .update(
                mapOf(
                    "${itemId}.status.${status}"
                            to dateFormat.format(Date())
                )
            )
    }

    fun setPrintedDate(orderId: String) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
        dateFormat.timeZone = TimeZone.getTimeZone("GMT");
        val itemReference = db.collection("Orders").document(orderId)
        itemReference
            .update(
                mapOf(
                    "printed"
                            to dateFormat.format(Date())
                )
            )

    }

    suspend fun updateOrderItemStatusFunction(
        restId: String,
        orderId: String,
        ros: String,
        uId: String,
        status: String
    ) {

        val map = hashMapOf(
            "orderId" to orderId,
            "restId" to restId!!,
            "ROSType" to ros,
            "itemUID" to uId,
            "statusType" to status,
        )
        Log.e(OrderItemsAdapter.TAG, "funAdapterResultPreparing: $map")

        FirebaseFunctions.getInstance("europe-west2")
            .getHttpsCallable("UpdateOrderItemStatusCF")
            .call(map)
            .continueWith { task ->
                val result = task.result?.data
                Log.e(OrderItemsAdapter.TAG, "funAdapterResult: $result")
                Utils.hideCustomLoadingDialog()
                result
            }

    }

}
