package com.ordergoapp

interface ListenToBadgeCount {
    fun onNewListeningToOrders(type: Int, count: Int)
}