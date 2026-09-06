package com.welsenho.pokedex.testing

import com.welsenho.pokedex.data.local.CaptureDao
import com.welsenho.pokedex.data.local.CaptureEntity
import com.welsenho.pokedex.data.local.CaptureWithPokemon
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Minimal stand-in; sync tests never touch captures. */
class FakeCaptureDao : CaptureDao {
    override suspend fun insert(capture: CaptureEntity): Long = 0
    override suspend fun delete(captureId: Long) = Unit
    override fun observeAll(): Flow<List<CaptureWithPokemon>> = flowOf(emptyList())
}
