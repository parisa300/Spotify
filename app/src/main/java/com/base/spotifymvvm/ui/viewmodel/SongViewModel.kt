package com.base.spotifymvvm.ui.viewmodel

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.base.spotifymvvm.exoplayer.MusicService
import com.base.spotifymvvm.exoplayer.MusicServiceConnection
import com.base.spotifymvvm.exoplayer.currentPlaybackPosition
import com.base.spotifymvvm.other.Constants.UPDATE_PLAYER_POSITION_INTERVAL
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

class SongViewModel @ViewModelInject constructor(
    musicServiceConnection: MusicServiceConnection
):ViewModel() {

    private val playbackState =musicServiceConnection.playbackState

    private val _curSongDuration =MutableLiveData<Long>()
    val curSongDuration :LiveData<Long> =_curSongDuration

    private val _curPlayerPosition =MutableLiveData<Long>()
    val curPlayerPosition :LiveData<Long> =_curPlayerPosition

    init {
        updateCurrentPlayerPosition()
    }

    private fun updateCurrentPlayerPosition(){
        viewModelScope.launch {
            while (true){
                val pos =playbackState.value?.currentPlaybackPosition
                if (curPlayerPosition.value !=pos){
                    _curPlayerPosition.postValue(pos)
                    _curSongDuration.postValue(MusicService.curSongDuration)

                }
                delay(UPDATE_PLAYER_POSITION_INTERVAL)
            }
        }
    }
}