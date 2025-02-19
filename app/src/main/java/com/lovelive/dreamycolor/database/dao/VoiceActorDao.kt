package com.lovelive.dreamycolor.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lovelive.dreamycolor.model.VoiceActorCard
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceActorDao {
    @Query("SELECT * FROM voice_actor_cards")
    fun getAllVoiceActors(): Flow<List<VoiceActorCard>>

    @Insert
    suspend fun insert(voiceActorCard: VoiceActorCard)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoiceActor(voiceActor: VoiceActorCard)

    @Query("DELETE FROM voice_actor_cards")
    suspend fun deleteAllVoiceActors()
}
