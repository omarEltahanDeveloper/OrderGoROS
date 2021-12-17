package com.ordergoapp.di.module

import android.content.Context
import com.ordergoapp.di.ApplicationScope
import com.ordergoapp.service.local.SessionManager
import dagger.Module
import dagger.Provides

@Module
class SessionModule(val context: Context) {
    @Provides
    @ApplicationScope
    internal fun getSessionManger(): SessionManager {
        return SessionManager(context)
    }
}