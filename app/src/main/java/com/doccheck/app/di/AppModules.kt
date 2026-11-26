package com.doccheck.app.di

import android.content.Context
import androidx.room.Room
import com.doccheck.app.BuildConfig
import com.doccheck.app.data.local.ChatDao
import com.doccheck.app.data.local.DocCheckDatabase
import com.doccheck.app.data.local.ScanDao
import com.doccheck.app.data.remote.DocCheckApiService
import com.doccheck.app.data.repository.AuthRepository
import com.doccheck.app.data.repository.AuthRepositoryImpl
import com.doccheck.app.data.repository.ChatRepository
import com.doccheck.app.data.repository.ChatRepositoryImpl
import com.doccheck.app.data.repository.ScanRepository
import com.doccheck.app.data.repository.ScanRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideDocCheckApiService(retrofit: Retrofit): DocCheckApiService {
        return retrofit.create(DocCheckApiService::class.java)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DocCheckDatabase {
        return Room.databaseBuilder(
            context,
            DocCheckDatabase::class.java,
            "doccheck.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideScanDao(db: DocCheckDatabase): ScanDao = db.scanDao()

    @Provides
    fun provideChatDao(db: DocCheckDatabase): ChatDao = db.chatDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindScanRepository(impl: ScanRepositoryImpl): ScanRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository
}

