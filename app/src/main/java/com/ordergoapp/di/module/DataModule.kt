package com.ordergoapp.di.module

import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class DataModule {
    @Singleton
    @Provides
    fun provideFirebaseFirestore(): FirebaseFirestore? {
        return FirebaseFirestore.getInstance()
    }


}