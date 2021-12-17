package com.ordergoapp

import android.content.Context
import com.ordergoapp.di.ApplicationScope

import dagger.Module
import dagger.Provides



@Module
public class ContextModule(private val context: Context) {
    @Provides
    @ApplicationScope
    @ApplicationContext
    fun providesContext(): Context {
        return context
    }
}