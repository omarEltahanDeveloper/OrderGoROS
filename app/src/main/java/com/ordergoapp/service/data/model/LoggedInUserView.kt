package com.ordergoapp.service.data.model

/**
 * User details post authentication that is exposed to the UI
 */
data class LoggedInUserView(
    var owner: Boolean = true,//Owner
    var restId: String? = "",//restaurant ID
    var ros: String? = ""
)