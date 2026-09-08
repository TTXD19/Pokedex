package com.welsenho.pokedex.ui.home

import com.welsenho.pokedex.data.repository.SyncState
import com.welsenho.pokedex.data.repository.TypeGroup

data class HomeUiState(
    val captured: List<CapturedItem> = emptyList(),
    val typeGroups: List<TypeGroup> = emptyList(),
    val syncState: SyncState = SyncState.Idle,
    val isRefreshing: Boolean = false,
    val isOnline: Boolean = true,
) {
    val showFullScreenError: Boolean
        get() = typeGroups.isEmpty() &&
            (syncState as? SyncState.Failed)?.pokemonListUnavailable == true

    companion object{
        data class CapturedItem(
            val captureId: Long,
            val pokemonId: Int,
            val name: String,
            val imageUrl: String?,
        )
    }
}
