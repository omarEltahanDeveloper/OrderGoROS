package com.ordergoapp.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ordergoapp.service.data.*
import com.ordergoapp.service.data.pojoModel.AppVersion
import com.ordergoapp.service.data.pojoModel.Restaurant
import com.ordergoapp.service.datasource.Resource
import com.ordergoapp.service.repositry.ROSRepositry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Exception

class OrdersViewModel() : ViewModel() {
    private lateinit var repositry: ROSRepositry;

    companion object {
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

    fun getPlacedItems():  MutableLiveData<Resource<List<ROSOrderItem>>> {
        // GlobalScope.launch(Dispatchers.IO) {
        viewModelScope.launch {
            // delay(3000L)
            placedItems = repositry.getPlacedItems()
        }
        return placedItems
    }

    fun getPlacedOrders(): MutableLiveData<List<OrderShape>> {
        //GlobalScope.launch(Dispatchers.IO) {
        viewModelScope.launch {
            //    delay(3000L)
            placedOrders = repositry.getPlacedOrders()
        }
        return placedOrders
    }

    fun getPlacedOrdersTrueTMA(): MutableLiveData<List<OrderShape>> {
        //GlobalScope.launch(Dispatchers.IO) {
        viewModelScope.launch {
               // delay(3000L)
            placedOrdersTrueTMA = repositry.getPlacedOrdersTrueTMA()
        }
        return placedOrdersTrueTMA
    }

    fun getPlacedOrdersFalseTMA(): MutableLiveData<List<OrderShape>> {
        //GlobalScope.launch(Dispatchers.IO) {
        viewModelScope.launch {
            //    delay(3000L)
            placedOrdersFalseTMA = repositry.getPlacedOrdersFalseTMA()
        }
        return placedOrdersFalseTMA
    }

    fun getCompletedOrders(): MutableLiveData<List<OrderShape>> {
        // GlobalScope.launch(Dispatchers.IO) {
        viewModelScope.launch {
            //    delay(3000L)
            completedOrders = repositry.getCompletedOrders()
        }
        return completedOrders
    }

    fun getTableOrders(tableId: Int): MutableLiveData<List<OrderShape>> {
        tableOrders = repositry.getTableOrders(tableId)
        return tableOrders
    }

    /* fun getSharedItemSession(): MutableLiveData<List<SharedItemSession>> {
         sharedItemSessions = repositry.getSharedItemsSessions()
         return sharedItemSessions
     }

     fun mergeSharedItemToItems() {
         val sessions = sharedItemSessions.value
         if (sessions != null) {
             for (session in sessions) {
                 for (orderItem in session.itemsList!!) {
                     val list = (completedOrders.value as List<OrderShape>)
                         .find { orderShape -> orderShape.orderId == orderItem.orderId }?.rosOrder?.itemsList
                     list?.removeIf { it.uid == orderItem.uid }

                     val placedOrdersList = (placedOrders.value as List<OrderShape>)
                         .find { orderShape -> orderShape.orderId == orderItem.orderId }?.rosOrder?.itemsList
                     placedOrdersList?.removeIf { it.uid == orderItem.uid }


                     val placedItemsList = (placedItems.value as ArrayList<ROSOrderItem>)
                     placedItemsList?.removeIf { it.uid == orderItem.uid }

                     if (!orderItem.status?.completed.isNullOrEmpty()
                         && orderItem.status?.delivered.isNullOrEmpty()
                     ) {

                         list?.add(orderItem)

                     } else {
                         val orderNum = (placedOrders.value as List<OrderShape>)
                             .find { orderShape -> orderShape.orderId == orderItem.orderId }?.order?.on
                         orderItem.onId=orderNum
                         placedOrdersList?.add(orderItem)
                         placedItemsList?.add(orderItem)
                     }

                 }


             }
         }

     }*/

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


//    private fun getOrderShape(i: Int): OrderShape {
//        return OrderShape(
//            "",
//            listOf("b_1", "k_1"),
//            mapOf(
//                "b_1" to arrayListOf(
//                    RosOrder(
//                        "",
//                        "Test",
//                        listOf<IngItem>(IngItem("1", "FataCheese ")),
//                        2.5,
//                        1,
//                        "Doner Wrap",
//                        "",
//                        "main course"
//                    ),
//                    RosOrder(
//                        "",
//                        "Test",
//                        listOf<IngItem>(IngItem("1", "FataCheese ")),
//                        2.5,
//                        1,
//                        "Doner Wrap",
//                        "",
//                        "main course"
//                    )
//                )
//            ),
//            mapOf("status_b_1" to Status("2021-02-23T2:48:13Z", "", "")),
//            "",
//            Order("1", on = i.toString(), tn = "1")
//        )
//
//
//    }


}