package com.ordergoapp.service.data


import com.ordergoapp.service.datasource.FirebaseDataSource

import java.io.Serializable


data class OrderShape(
    var orderId: String? = null,
    val csa: Boolean? = false,
    val ROSTypes: List<String?>? = null,
    val canceled: String = "",
    val date: String = "",
    val dateTime: String = "",
    val isDelivered: Boolean = false,
    val is_paid_delivered: Boolean = false,
    val order: Order? = null,
    val paid: String = "",
    val paidMobile: String = "",
    val refund: String = "",
    val rejected: String = "",
    val rta: Boolean = false,
    var rosOrder: RosOrder? = null,
    val time: String = "",
    var isTotalCompleted: Boolean = false,
    var docStatus: Int = 0,
    var printed: String = "",
    var guest: Guest? = null,
    var sharedItems: List<ROSOrderItem>? = null
) : Serializable {

}

data class SharedItemInfo(
    var ROSType: String? = null,
    var csm: String? = null,
    var itemId: String? = null,
    var sessionRef: String? = null
) : Serializable

data class Guest(
    var number: Int = 0,
    var total: Int = 0
) : Serializable

data class Status(
    var placed: String? = "",
    var preparing: String? = "",
    var completed: String? = "",
    var delivered: String? = ""
) : Serializable

data class IngItem(
    val price: Double = 0.0,
    val name: String? = "",
    val id: String? = ""
) : Serializable {
    override fun toString(): String {
        return "- $name"
    }
}

data class ROSOrderItem(
    val catId: String? = "",
    val notes: String? = "",
    val option: String? = "",
    val ing: List<IngItem?>? = null,
    val price: Double = 0.0,
    val QTY: Long = 0,
    val QTY_shared: Double = 0.0,
    val name: String = "",
    val ROSType: String? = "",
    val csm: String = "",
    var is_shared: Boolean = false,
    val id: String? = "",
    val serving: String? = "",
    val total_price: Double = 0.0,
    val total_price_shared: Double = 0.0,
    val uid: String = "",
    val logo: String = "",
    val status: Status? = null,
    var onId: Long? = null,
    var tableId: Long? = null,
    var ros: String = "",
    var rosType: String = "",
    var restId: String = "",
    var main_shared_phone: String = "",
    var orderId: String? = null,
    var sessionRef: String = "",
    val itemId: String = ""
) : Serializable {

}

data class RosOrder(
    var items: List<String>? = null,
    var itemsList: ArrayList<ROSOrderItem>? = null
) : Serializable

data class Order(
    val restId: String? = "",
    val restName: String? = "",
    val VAT: Long? = null,
    val VATn: String? = null,
    val VATa: Double? = null,
    val city: String? = "",
    val csm: String? = "",
    val discount: Double = 0.0,
    val dm: Int = 0,
    val is_checkout: Boolean = false,
    val logo: String = "",
    val nta: Double = 0.0,
    val on: Long = 0,
    val pm: Int = 0,
    val son: Long = 0,
    val subTotal: Double = 0.0,
    val total: Double = 0.0,
    val tn: Long = 0,
    val tip: Double = 0.0,
) : Serializable

