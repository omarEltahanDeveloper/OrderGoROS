package com.ordergoapp.service.local

import android.content.Context
import android.content.SharedPreferences
import com.ordergoapp.R
import com.ordergoapp.service.data.model.LoggedInUserView


class SessionManager(context: Context) {

    private var prefs: SharedPreferences =
        context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE)

    companion object {
        const val RESID = "resId"
        const val ROS = "ros"
        const val ROSType = "rosType"
        const val ROSVersion = "rosVersion"
        const val ISOWNER = "isOwner"
        const val USER_ISLOGGED = "isLoggedIn"
        const val USER_TOKEN = "userToken"
        const val PID = "pid"
        const val RES_N_TABLE = "res_nTable"
        const val AUTO_PRINT = "auto_print"
        var nTable: Int = 0
        var autoPrint: Boolean = false
    }

    interface NotifyNTableListener {
        fun nTableChange(old: Int, new: Int)
    }

    /**
     * Function to save auth token
     */
    fun saveAuthToken(token: String?) {

        val editor = prefs.edit()
        editor.putString(USER_TOKEN, token)
        editor.apply()
    }

    /**
     * Function to fetch auth token
     */
    fun fetchAuthToken(): String? {
        return prefs.getString(USER_TOKEN, null)
    }

    /**
     * Function to save auto_print
     */
    fun saveAutoPrint(autoPrint: Boolean) {
        val editor = prefs.edit()
        editor.putBoolean(AUTO_PRINT, autoPrint)
        editor.apply()
        Companion.autoPrint =autoPrint
    }

    /**
     * Function to fetch auto_print
     */
    fun fetchAutoPrint(): Boolean {
        autoPrint = prefs.getBoolean(AUTO_PRINT, false)
        return autoPrint
    }

    /**
     * Function to save restaurant table number
     */
    fun saveResNTable(nTable: Int) {

        val editor = prefs.edit()
        editor.putInt(RES_N_TABLE, nTable)
        editor.apply()
    }

    /**
     * Function to fetch restaurant table number
     */
    fun fetchResNTable(): Int {
        nTable = prefs.getInt(RES_N_TABLE, 0)
        return nTable
    }


    /**
     * Function to save pid
     */
    fun savePid(pid: Boolean) {

        val editor = prefs.edit()
        editor.putBoolean(PID, pid)
        editor.apply()
    }

    /**
     * Function to fetch auth token
     */
    fun fetchPid(): Boolean{
        return prefs.getBoolean(PID, true)
    }

    /**
     * Function to save restaurant id
     */
    fun saveRESID(resId: String?) {
        val editor = prefs.edit()
        editor.putString(RESID, resId)
        editor.apply()
    }

    /**
     * Function to fetch restaurant id
     */
    fun fetchRESID(): String? {
        return prefs.getString(RESID, "")
    }

    /**
     * Function to save ROS
     */
    fun saveROS(ros: String?) {
        val editor = prefs.edit()
        editor.putString(ROS, ros)
        editor.apply()
    }

    /**
     * Function to fetch ROS
     */
    fun fetchROS(): String? {
        return prefs.getString(ROS, "")
    }

    /**
     * Function to save ROS Type
     */
    fun saveROSType(ros: String?) {
        val editor = prefs.edit()
        editor.putString(ROSType, ros)
        editor.apply()
    }

    /**
     * Function to fetch ROS Type
     */
    fun fetchROSType(): String? {
        return prefs.getString(ROSType, "")
    }

    /**
     * Function to save ROS
     */
    fun saveROSVersion(ros: Int?) {
        val editor = prefs.edit()
        editor.putInt(ROSVersion, ros!!)
        editor.apply()
    }

    /**
     * Function to fetch ROS
     */
    fun fetchROSVersion(): Int? {
        return prefs.getInt(ROSVersion, 0)
    }

    /**
     * Function to user is logged-in
     */
    fun saveIsLoggedIn(isLoggedIn: Boolean) {
        val editor = prefs.edit()
        editor.putBoolean(USER_ISLOGGED, isLoggedIn)
        editor.apply()
    }

    /**
     * Function to fetch  is logged-in
     */
    fun fetchIsLoggedIn(): Boolean {
        return prefs.getBoolean(USER_ISLOGGED, false)
    }

    /**
     * Function to user is owner
     */
    fun saveIsOwner(isOwner: Boolean) {
        val editor = prefs.edit()
        editor.putBoolean(ISOWNER, isOwner)
        editor.apply()
    }

    /**
     * Function to fetch  is owner
     */
    fun fetchIsOwner(): Boolean {
        return prefs.getBoolean(ISOWNER, false)
    }

    fun fetchLoggedInUser(): LoggedInUserView {
        return LoggedInUserView(fetchIsOwner(), fetchRESID(), fetchROS());
    }

}