package com.ordergoapp.service.data

data class ROS(
    var id: String = "",
    var number: Int = 0,
    var name: String = "",
    var status: Boolean = true,
    var type: String = ""
) {

    override fun toString(): String {
        return if (!ROSUtils.ROSTypes[type].equals("t")) {
            ROSUtils.ROSTypes[type] + " " + number
        } else ROSUtils.ROSTypes[type] + ""
    }
}

class ROSUtils {
    companion object {
        val ROSTypes = hashMapOf(
            "b" to "Bar",
            "k" to "Kitchen",
            "om" to "OrderMonitoring",
            "st" to "Starters",
            "s" to "Sweet",
            "t" to "Takeaway"
        )

        fun getResName(ros: String): String {
            val temp = ros.split("_")
            return if (temp[0] != "t") {
                ROSTypes[temp[0]] + " " + temp[1]
            } else ROSTypes[temp[0]] + ""

        }
    }

}
