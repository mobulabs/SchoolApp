package com.specialschool.schoolapp.di

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.firestoreSettings
import com.google.firebase.ktx.Firebase
import com.specialschool.schoolapp.MainApplication
import com.specialschool.schoolapp.data.db.AppDatabase
import com.specialschool.schoolapp.data.SchoolRepository
import com.specialschool.schoolapp.data.SchoolDataSource
import com.specialschool.schoolapp.data.bootstrap.BootstrapSchoolDataSource
import com.specialschool.schoolapp.data.remote.RemoteSchoolDataSource
import com.specialschool.schoolapp.data.userevent.DefaultSchoolAndUserItemRepository
import com.specialschool.schoolapp.data.userevent.FirestoreUserEventDataSource
import com.specialschool.schoolapp.data.userevent.SchoolAndUserItemRepository
import com.specialschool.schoolapp.data.userevent.UserEventDataSource
import com.specialschool.schoolapp.domain.search.FtsQueryMatchStrategy
import com.specialschool.schoolapp.domain.search.QueryMatchStrategy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import javax.inject.Named
import javax.inject.Singleton

@InstallIn(ApplicationComponent::class)
@Module
class AppModule {

    @Singleton
    @Provides
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.buildDatabase(context)
    }

    @Singleton
    @Provides
    @Named("remoteSchoolDataSource")
    fun provideRemoteSchoolDataSource(
        @ApplicationContext context: Context
    ): SchoolDataSource {
        return RemoteSchoolDataSource(context)
    }

    @Singleton
    @Provides
    @Named("bootstrapSchoolDataSource")
    fun provideBootstrapSchoolDataSource(): SchoolDataSource {
        return BootstrapSchoolDataSource
    }

    @Singleton
    @Provides
    fun provideSchoolRepository(
        @Named("remoteSchoolDataSource") remoteDataSource: SchoolDataSource,
        @Named("bootstrapSchoolDataSource") bootstrapDataSource: SchoolDataSource,
        database: AppDatabase
    ): SchoolRepository {
        return SchoolRepository(remoteDataSource, bootstrapDataSource, database)
    }

    @ExperimentalCoroutinesApi
    @Singleton
    @Provides
    fun provideUserItemDataSource(
        firestore: FirebaseFirestore,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ): UserEventDataSource {
        return FirestoreUserEventDataSource(firestore, dispatcher)
    }

    @ExperimentalCoroutinesApi
    @Singleton
    @Provides
    fun provideSchoolAndUserItemRepository(
        userEventDataSource: UserEventDataSource,
        schoolRepository: SchoolRepository
    ): SchoolAndUserItemRepository {
        return DefaultSchoolAndUserItemRepository(userEventDataSource, schoolRepository)
    }

    @Singleton
    @Provides
    fun provideQueryMatchStrategy(appDatabase: AppDatabase): QueryMatchStrategy {
        return FtsQueryMatchStrategy(appDatabase)
    }

    @Provides
    fun provideContext(application: MainApplication): Context {
        return application.applicationContext
    }

    @ApplicationScope
    @Singleton
    @Provides
    fun providesApplicationScope(
        @DefaultDispatcher dispatcher: CoroutineDispatcher
    ): CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher)

    @Singleton
    @Provides
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return Firebase.firestore.apply {
            firestoreSettings = firestoreSettings { isPersistenceEnabled = true }
        }
    }
}
