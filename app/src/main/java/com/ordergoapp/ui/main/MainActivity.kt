package com.ordergoapp.ui.main

import android.app.AlertDialog
import android.app.PendingIntent
import android.content.*
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Parcelable
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.dantsu.escposprinter.connection.DeviceConnection
import com.dantsu.escposprinter.connection.usb.UsbConnection
import com.dantsu.escposprinter.connection.usb.UsbPrintersConnections
import com.dantsu.escposprinter.textparser.PrinterTextParserImg
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.functions.FirebaseFunctions
import com.google.gson.Gson
import com.ordergoapp.ListenToBadgeCount
import com.ordergoapp.R
import com.ordergoapp.async.AsyncEscPosPrinter
import com.ordergoapp.async.AsyncUsbEscPosPrint
import com.ordergoapp.databinding.ActivityMainBinding
import com.ordergoapp.service.data.OrderShape
import com.ordergoapp.service.data.ROSOrderItem
import com.ordergoapp.service.data.pojoModel.AppVersion
import com.ordergoapp.service.data.pojoModel.Restaurant
import com.ordergoapp.service.datasource.FirebaseDataSource
import com.ordergoapp.service.datasource.Status
import com.ordergoapp.service.local.SessionManager
import com.ordergoapp.service.repositry.ROSRepositry
import com.ordergoapp.ui.adapter.OrderItemsAdapter
import com.ordergoapp.ui.login.LoginActivity
import com.ordergoapp.ui.tables.TableOrderFragment
import com.ordergoapp.utils.Utils
import com.ordergoapp.utils.Variables
import com.ordergoapp.viewmodel.OrdersViewModel
import java.util.*
import kotlin.properties.Delegates


class MainActivity : AppCompatActivity() , ListenToBadgeCount {

    private val TAG = MainActivity::class.java.name
    private lateinit var binding: ActivityMainBinding

    private lateinit var tone: TimerTask
    private lateinit var timer: Timer

    private lateinit var ordersViewModel: OrdersViewModel
    private lateinit var txt_noInternet: TextView
    lateinit var navView: BottomNavigationView
    private lateinit var sessionManager: SessionManager

    private lateinit var backPressedToast: Toast
    private var backPressedTime: Long = 0

    companion object {
        private var completedTabTimer: CountDownTimer? = null
        var isCompletedTabOpened: Boolean by Delegates.observable(false) { prop, old, new ->
        }

        private var printedOrder: OrderShape? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //Utils.showCustomLoadingDialog(this)

        initUi()
        initViewModel()
        initUIAndActions()
        initData()


    }

    override fun onBackPressed() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment?
        when (navHostFragment?.let { NavHostFragment.findNavController(it).currentDestination?.id }) {
            R.id.navigation_placedItems -> {
                ExsitFromApp()
            }
            else -> {
                super.onBackPressed()
            }
        }
    }

    private fun ExsitFromApp() {
        backPressedToast =
            Toast.makeText(this, R.string.message_back_pressed, Toast.LENGTH_LONG)

        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            backPressedToast.cancel()
            //super.onBackPressed()
            finishAffinity()
            return
        } else {
            backPressedToast.show()
        }
        backPressedTime = System.currentTimeMillis()
    }

    private fun initUi() {
        navView = binding.navView //findViewById(R.id.nav_view)
        txt_noInternet =
            binding.includeTextNoInternet.txtNoInternetConnection //findViewById(R.id.txt_noInternetConnection)
        sessionManager = SessionManager(this)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment?
        val navController = navHostFragment!!.navController
        NavigationUI.setupWithNavController(navView, navController)
    }

    private fun initViewModel() {
        //region Get Ros Orders and assign Tabs Count
        ordersViewModel =
            ViewModelProvider(this).get(OrdersViewModel::class.java)

        ordersViewModel.assignRepositry(
            ROSRepositry(
                FirebaseDataSource(sessionManager),
                sessionManager
            )
        )

    }

    private fun initUIAndActions() {
        //Check Internet Connectivity Listener
        Variables.networkNotifyListeners.add(object : Variables.InterfaceNetworkNotify {
            override fun networkChange(old: Boolean, new: Boolean) {
                if (new) {
                    runOnUiThread { txt_noInternet.visibility = View.GONE }
                } else {
                    runOnUiThread { txt_noInternet.visibility = View.VISIBLE }
                }
            }

        })

    }

    private fun initData() {

        SessionManager.autoPrint = sessionManager.fetchAutoPrint()

        getRestaurantInfo()

        getCurrentAppVersion()

        //region Completed Tab Timer
        Variables.completedTabListener = object : Variables.InterfaceIsCompletedTab {
            override fun completedTabChanges(old: Boolean, new: Boolean) {
                changeCompletedTabTimer(old, new)
            }
        }

        //playSoundPlacedItems()

    }

    fun getRestaurantInfo() {
        ordersViewModel.getRestaurantInfo()
        OrdersViewModel.restaurantInfo.observe(this, {
            if (it.confirmed)
                initTabs()
            else {
                val sessionManager = SessionManager(this.applicationContext)
                sessionManager.saveIsLoggedIn(false)
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }

        })
    }

    fun getCurrentAppVersion() {
        ordersViewModel.getCurrentAppVersion()
        OrdersViewModel.versionInfo.observe(this, {
            if (it.version.isNotEmpty() && it.version != Utils.getVersionName(this))
                Utils.updateApp(this)
        })
    }

    //region Play Sound For Placed Items
    private fun playSoundPlacedItems() {
        timer = Timer("MetronomeTimer", true)
        tone = object : TimerTask() {
            override fun run() {
                try {
                    //Play sound
                    if (Variables.placedOrdersHasPlacedItems > 0)
                        playBeepSound()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
        }
        timer.scheduleAtFixedRate(tone, 45000, 45000)

    }

    //endregion

    override fun onStart() {
        super.onStart()
        getAvailableTableNumbers()
    }

    private fun getAvailableTableNumbers() {
        val map = hashMapOf(
            "restId" to sessionManager.fetchRESID()
        )
        FirebaseFunctions.getInstance("europe-west2")
            .getHttpsCallable("GetRestaurantConfirmedCF")
            .call(map)
            .continueWith { task ->
                if (task.exception != null) {
                    Log.w(TAG, "listen:error", task.exception)
                    return@continueWith
                }
                val result = task.result?.data
                parseDataAndSave(result)
                result
            }
    }

    private fun parseDataAndSave(result: Any?) {
        val json = Gson().toJson(result)
        val restaurantData = Gson().fromJson(json, Restaurant::class.java)
        Log.e(TAG, "${restaurantData.nTables}")

        sessionManager.savePid(restaurantData.pid)
        sessionManager.saveResNTable(restaurantData.nTables)
        SessionManager.nTable = restaurantData.nTables

    }

    fun playBeepSound() {
        try {
            val notification: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val r = RingtoneManager.getRingtone(applicationContext, notification)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                r.volume = 1.0f
            }
            r.play()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    fun initialPlacedItems() {
        val placedItems = OrdersViewModel.placedItems
        placedItems.observe(this, {
            when(it.status) {
                Status.SUCCESS -> {
                    if (it.data != null && it.data.size > 0)
                        onNewListeningToOrders(0,it.data.size)
                    else
                        onNewListeningToOrders(0,0)
                    Utils.hideCustomLoadingDialog()
                }
                Status.ERROR -> {
                    Snackbar.make(
                        this.binding.container,
                        it.message?: "An unknown error occured.",
                        Snackbar.LENGTH_LONG
                    ).show()
                    Utils.hideCustomLoadingDialog()
                }
                Status.LOADING -> {
                    Utils.showCustomLoadingDialog(this)
                }
            }
        })
    }
    fun initialPlacedOrders() {
        val placedOrder =  ordersViewModel.getAllPlacedOrders(false)
        placedOrder.observe(this, {
            val count =
                placedOrder.value?.filter { orderShape -> orderShape.rosOrder?.itemsList?.size!! > 0 }!!.size
            onNewListeningToOrders(1,count)
            Variables.placedOrdersCount = placedOrder.value?.count {
                var hasPreparing = false
                for (item in it?.rosOrder?.itemsList!!) {
                    if (!item.status?.preparing.isNullOrEmpty())
                        hasPreparing = true
                }
                !hasPreparing
            }!!
        })
    }
    fun initialPlacedCompletedOrders() {
        val completedOrders = ordersViewModel.getAllPlacedOrders(true)
        completedOrders.observe(this, {
            val count =
                completedOrders.value?.filter { orderShape -> orderShape.rosOrder?.itemsList?.size!! > 0 }!!.size
            onNewListeningToOrders(2,count)
        })
    }
    private fun initTabs() {
        val modeldata = ordersViewModel.getAllOrdersAsSnapShotfFalse(sessionManager)
        modeldata.observe(this, {
            val modeldata2 = ordersViewModel.getAllOrdersAsSnapShotTrue(sessionManager)
            modeldata2.observe(this, {
                ordersViewModel.setAllDataToRetrieved()
            })
        })
        initialPlacedItems()
        initialPlacedOrders()
        initialPlacedCompletedOrders()


        onNewListeningToOrders(3,sessionManager.fetchResNTable())

        if (sessionManager.fetchROSType().equals("t"))
            navView.menu.get(3).isVisible = false
    }

    fun changeCompletedTabTimer(old: Boolean, new: Boolean) {
        if (new) {
            completedTabTimer = object : CountDownTimer(60000, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                }

                override fun onFinish() {
                    findNavController(R.id.nav_host_fragment).navigate(R.id.navigation_placedOrders)
                }
            }.start()
        } else {
            if (completedTabTimer != null) {
                completedTabTimer?.cancel();
                completedTabTimer = null
            }
        }
    }

    // region Printer
    private val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"

    private val usbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            try {
                val action = intent.action
                val orderStr = intent.getStringExtra("orderStr")
                if (ACTION_USB_PERMISSION == action) {
                    synchronized(this) {
                        val usbManager =
                            getSystemService(Context.USB_SERVICE) as UsbManager
                        val usbDevice =
                            intent.getParcelableExtra<Parcelable>(UsbManager.EXTRA_DEVICE) as UsbDevice?
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            if (usbDevice != null) {

                                AsyncUsbEscPosPrint(context)
                                    .execute(
                                        printedOrder?.let {
                                            getAsyncEscPosPrinter(
                                                UsbConnection(
                                                    usbManager,
                                                    usbDevice
                                                ), getPrintedStr(printedOrder)
                                            )
                                        }
                                    )
                                printedOrder = null
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                AlertDialog.Builder(context)
                    .setTitle("USB Connection")
                    .setMessage(e.stackTraceToString())
                    .show()

            }
        }
    }


    fun printUsb(order: OrderShape, isAutoPrint: Boolean): Boolean {
        try {
            val usbConnection: UsbConnection? = UsbPrintersConnections.selectFirstConnected(this)
            val usbManager = this.getSystemService(Context.USB_SERVICE) as UsbManager
            //   printedOrder = order;
            if (usbConnection == null || usbManager == null) {
                if (!isAutoPrint)
                    AlertDialog.Builder(this)
                        .setTitle("USB Connection")
                        .setMessage("No USB printer found.")
                        .show()

                return false
            }
            if (usbConnection != null) {
                val intent = Intent(ACTION_USB_PERMISSION);
                intent.putExtra("orderStr", getPrintedStr(order))
                val permissionIntent =
                    PendingIntent.getBroadcast(this, 0, intent, 0)

                val filter = IntentFilter(ACTION_USB_PERMISSION)
                registerReceiver(usbReceiver, filter)
                if (!usbManager.hasPermission(usbConnection.device)) {

                    printedOrder = order
                    usbManager.requestPermission(usbConnection.device, permissionIntent)

                } else {
                    AsyncUsbEscPosPrint(this)
                        .execute(
                            getAsyncEscPosPrinter(usbConnection, getPrintedStr(order))
                        )

                }
                return true

            }
        } catch (e: Exception) {
            if (!isAutoPrint)
                AlertDialog.Builder(this)
                    .setTitle("USB Connection")
                    .setMessage(e.stackTraceToString())
                    .show()
            return false
        }
        return false
    }

    // var printedOrder: OrderShape? = null

    /**
     * Asynchronous printing
     */
    fun getAsyncEscPosPrinter(
        printerConnection: DeviceConnection?, orderStr: String
    ): AsyncEscPosPrinter? {
        try {
            val printer = AsyncEscPosPrinter(printerConnection, 203, 58f, 48)
            var str = "[C]<img>" + PrinterTextParserImg.bitmapToHexadecimalString(
                printer,
                this.applicationContext.resources
                    .getDrawableForDensity(
                        R.drawable.new_app_logo_printer,
                        DisplayMetrics.DENSITY_MEDIUM,
                        theme
                    )
            ) + "</img>\n"

            str += orderStr
            return printer.setTextToPrint(
                str.trimIndent()
            )
        } catch (e: Exception) {
            AlertDialog.Builder(this)
                .setTitle("USB Connection")
                .setMessage(e.stackTraceToString())
                .show()
        }
        return null
    }

    private fun getPrintedStr(printedOrder: OrderShape?): String {
        var str1 = ""
        str1 += """
                [C]<b><u>ORDER NO.</u>${printedOrder?.order?.on} - ${
            printedOrder?.order?.csm?.length?.let {
                printedOrder?.order?.csm?.substring(
                    it - 4,
                    it
                )
            }
        }    ${if (printedOrder?.order?.tn == 0L) "<u>TABLE NO.</u>Takeaway" else "<u>TABLE NO.</u>" + printedOrder?.order?.tn}</b>
                [C]================================
                """.trimIndent();
        for (item in printedOrder?.rosOrder?.itemsList!!) {
            str1 += "" +
                    "\n[L]<b>${item.QTY} X ${item.name}</b>[R]<font color='bg-black'> ${item.serving} </font> ${if (printedOrder?.order?.tn == 0L) "[R]£ " + item.total_price else ""}"
            for (ing in item?.ing!!) {
                str1 += "\n[L]<font size='normal'>  - ${ing?.name}</font> "
            }
            if (!item?.notes.isNullOrBlank()) {
                str1 += "\n[L]<u>Notes:</u>${item.notes}"
            }
            str1 += """
                [L]
                  """
        }
        if (printedOrder.order?.tn == 0L || printedOrder.order?.tn!! > 0L) {
            str1 += "\n[C]--------------------------------\n" +
                    "[L]Sub Total :[R]£${printedOrder.order?.subTotal}\n" +
                    "[L]Discount(${printedOrder.order?.discount})% :[R]£ -${"%.2f".format((printedOrder.order?.discount * printedOrder.order?.subTotal) / 100)}\n" +
                    "[L]Total  :[R]£${printedOrder.order?.total}\n" +
                    "[L]Tip  :[R]£${printedOrder.order?.tip}\n" +
                    "[L]VATn (${printedOrder.order?.VATn})  :[R] \n"
            str1 += "[C]--------------------------------\n" +
                    "[L]<b>Net Amount :<b>[R]£ ${printedOrder.order?.nta}\n"
        }

        str1 += """
                [L]
                [L]
                  """
        return str1
    }

    //  "[L]VAT Amount(${printedOrder.order?.VAT})%  :[R]£ ${printedOrder.order?.VATa}\n" +

    // endregion

    override fun onResume() {
        super.onResume()
//        try {
//            val appVersion = OrdersViewModel.versionInfo.value as AppVersion
//            if (appVersion.version.isNotEmpty() && appVersion.version != Utils.getVersionName(this))
//             Utils.updateApp(this)
//        } catch (e: Exception) {
//        }

    }

    override fun onNewListeningToOrders(type: Int, count: Int) {
        val idOfView : Int
        if (type == 0) {
            idOfView = R.id.navigation_placedItems
            Variables.placedOrdersHasPlacedItems = count
        }
        else if (type == 1) {
            idOfView = R.id.navigation_placedOrders
            Variables.placedOrdersCount = count
        }
        else if (type == 2) {
            idOfView = R.id.navigation_completedOrders
        }
        else {
            idOfView = R.id.navigation_tables
        }
        val badgeView = this.navView.getOrCreateBadge(idOfView)
        badgeView.maxCharacterCount = 3
        badgeView.badgeGravity = BadgeDrawable.BOTTOM_END
        badgeView.isVisible = count > 0
        badgeView.number = count
        if (count == 0) badgeView.clearNumber()
    }
}