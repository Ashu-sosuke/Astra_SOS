package com.example.sos.location.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CrimeIncident::class], version = 2, exportSchema = false)
abstract class CrimeDatabase : RoomDatabase() {

    abstract fun crimeDao(): CrimeDao

    companion object {
        @Volatile
        private var INSTANCE: CrimeDatabase? = null

        fun getInstance(context: Context): CrimeDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    CrimeDatabase::class.java,
                    "crime_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}