package com.ordergoapp.service.repositry

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.QuerySnapshot
import com.ordergoapp.service.local.SessionManager
import com.ordergoapp.service.data.*
import com.ordergoapp.service.data.model.LoggedInUserView
import com.ordergoapp.service.data.pojoModel.Restaurant
import com.ordergoapp.service.datasource.FirebaseDataSource
import com.ordergoapp.service.datasource.Resource


class ROSRepositry(val dataSource: FirebaseDataSource, val sessionManager: SessionManager) {


    init {
        if (sessionManager.fetchIsLoggedIn()) {
            setLoggedInUser(sessionManager.fetchLoggedInUser())
        }
    }

    // in-memory cache of the loggedInUser object
    companion object {
        var user: LoggedInUserView? = null
            private set

        val isLoggedIn: Boolean
            get() = user != null

        fun setLoggedInUser(loggedInUser: LoggedInUserView) {
            this.user = loggedInUser

            // If user credentials will be cached in local storage, it is recommended it be encrypted
            // @see https://developer.android.com/training/articles/keystore
        }


    }

    fun getAllROS() =
        dataSource.getAllROS(user?.restId!!)

     suspend fun getPlacedItems(): MutableLiveData<Resource<List<ROSOrderItem>>> =
        dataSource.getItems2()

     suspend fun getAllOrdersAsSnapShotTrue(): MutableLiveData<QuerySnapshot> =
        dataSource.getAllOrdersAsSnapShotTrue()

     suspend fun getAllOrdersAsSnapShotFalse(): MutableLiveData<QuerySnapshot> =
        dataSource.getAllOrdersAsSnapShotFalse()

     suspend fun listenToOrdersTrueTMA(): MutableLiveData<QuerySnapshot> =
        dataSource.listenToOrdersTrueTMA()

     suspend fun listenToOrdersFalseTMA(): MutableLiveData<QuerySnapshot> =
        dataSource.listenToOrdersFalseTMA()


      suspend fun getPlacedOrdersTrueTMA(isCompleted: Boolean): MutableLiveData<List<OrderShape>> =
        dataSource.ListenToLatestAddedTrueTMA(isCompleted)

     suspend fun getPlacedOrdersFalseTMA(isCompleted: Boolean): MutableLiveData<List<OrderShape>> =
        dataSource.ListenToLatestAddedFalseTMA(isCompleted)

    suspend fun getAllPlacedOrders(isCompeleted: Boolean): MutableLiveData<List<OrderShape>> =
        dataSource.getAllOrdersForFirstTime(isCompeleted)

    fun getTableOrders(tableId: Int): MutableLiveData<List<OrderShape>> =
        dataSource.getAllOrdersByTable2(false, tableId)

    fun getRestaurantInfo(): MutableLiveData<Restaurant>? =
        sessionManager.fetchRESID()?.let { dataSource.getRestaurantInfo(it) }

    fun getCurrentROSVersion() =
        dataSource.getCurrentROSVersion()

    fun saveSetupInfo(context: Context, ros: ROS) {
        val sessionManager = SessionManager(context)
        sessionManager.saveIsLoggedIn(true)
        sessionManager.saveRESID(user?.restId)
        sessionManager.saveROS(ros.type + "_" + ros.number)
        sessionManager.saveROSType(ros.type)
        sessionManager.saveROSVersion(ros.number)
        user?.restId?.let { dataSource.getRestaurantInfo(it) }
    }

    fun updateItemStatus(orderId: String, ros: String, itemId: String, status: String) {
        dataSource.updateItemStatus(orderId, ros, itemId, status)
    }

    suspend fun updateItemStatusFunction(orderId: String, ros: String, uid: String, status: String) {
        dataSource.updateOrderItemStatusFunction(user?.restId!!, orderId, ros, uid, status)
    }

    fun updateSharedItemStatus(sessionId: String, itemId: String, status: String) {
        dataSource.updateSharedItemStatus(sessionId, itemId, status)
    }

    fun setPrintedDate(orderId: String) {
        dataSource.setPrintedDate(orderId)
    }

    fun updateROSStatus(resID: String, rosId: String) {
        dataSource.updateROSStatus(resID, rosId)
    }
}
