package com.example.sos.location.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CrimeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(incidents: List<CrimeIncident>)

    @Query("SELECT * FROM crimes ORDER BY date DESC")
    suspend fun getAllIncidents(): List<CrimeIncident>

    @Query("SELECT * FROM crimes WHERE category = :category ORDER BY date DESC")
    suspend fun getByCategory(category: String): List<CrimeIncident>

    @Query("DELETE FROM crimes WHERE date < :cutoff")
    suspend fun deleteOlderThan(cutoff: String)

    @Query("SELECT COUNT(*) FROM crimes")
    suspend fun getCount(): Int

}