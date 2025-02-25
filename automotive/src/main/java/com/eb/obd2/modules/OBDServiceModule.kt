package com.eb.obd2.modules

import android.content.Context
import com.eb.obd2.services.OBDConnectionFactory
import com.eb.obd2.services.OBDConnectionType
import com.eb.obd2.services.OBDService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OBDServiceModule {

    @Provides
    @Singleton
    fun provideOBDConnectionFactory(@ApplicationContext context: Context): OBDConnectionFactory {
        return OBDConnectionFactory(context)
    }

    @Provides
    @Singleton
    fun provideOBDService(factory: OBDConnectionFactory): OBDService {
        val initialConnection = factory.create(OBDConnectionType.TCP)
        return OBDService(initialConnection)
    }
}