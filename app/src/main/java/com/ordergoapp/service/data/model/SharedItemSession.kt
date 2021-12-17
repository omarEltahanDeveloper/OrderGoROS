package com.ordergoapp.service.data.model

import com.ordergoapp.service.data.ROSOrderItem

data class SharedItemSession(
    var checkout_phones:List<String>?=null,
    var shared_items: List<String>? = null,
    var itemsList: List<ROSOrderItem>? = null,
    var main_shared_phone:String="",
    var table_number:Long=0,
    var phones:List<String>?=null
)
