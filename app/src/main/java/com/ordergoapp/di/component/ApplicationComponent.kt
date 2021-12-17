package com.ordergoapp.di.component

import android.content.Context
import com.ordergoapp.ApplicationContext
import com.ordergoapp.ContextModule
import com.ordergoapp.MyApplication
import com.ordergoapp.di.ApplicationScope
import com.ordergoapp.di.module.SessionModule
import com.ordergoapp.service.local.SessionManager
import dagger.Component


@ApplicationScope
@Component(modules = [ContextModule::class,SessionModule::class])
interface ApplicationComponent {
    @get:ApplicationContext
    val context: Context
    fun sessionManager(): SessionManager
    fun injectApplication(application: MyApplication)
}