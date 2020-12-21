package com.base.spotifymvvm.ui.viewmodel

import android.media.browse.MediaBrowser
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_MEDIA_ID
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.base.spotifymvvm.data.entitie.Song
import com.base.spotifymvvm.exoplayer.MusicServiceConnection
import com.base.spotifymvvm.exoplayer.isPlayEnabled
import com.base.spotifymvvm.exoplayer.isPlaying
import com.base.spotifymvvm.exoplayer.isPrepared
import com.base.spotifymvvm.other.Constants.MEDIA_ROOT_ID
import com.base.spotifymvvm.other.Resource

class MainViewModel @ViewModelInject constructor(

    private val musicServiceConnection: MusicServiceConnection
):ViewModel() {

    private val _mediaItems =MutableLiveData<Resource<List<Song>>>()
    val mediaItems : LiveData<Resource<List<Song>>> = _mediaItems

    val isConnected =musicServiceConnection.isConnected
    val networkError =musicServiceConnection.netwokError
    val curPlayingSong=musicServiceConnection.curPlayingSong
    val playbackState =musicServiceConnection.playbackState

    init {
        _mediaItems.postValue(Resource.loading(null))
        musicServiceConnection.subscribe(MEDIA_ROOT_ID,object :MediaBrowserCompat.SubscriptionCallback()
        {

            override fun onChildrenLoaded(
                parentId: String,
                children: MutableList<MediaBrowserCompat.MediaItem>
            ) {
                super.onChildrenLoaded(parentId, children)

                val items=children.map {
                    Song(

                        it.mediaId!!,
                        it.description.title.toString(),
                        it.description.subtitle.toString(),
                        it.description.mediaUri.toString(),
                        it.description.iconUri.toString()
                    )
                }

                _mediaItems.postValue(Resource.success(items))
            }
        })
    }



    fun skipToNextSong(){
        musicServiceConnection.transportControls.skipToNext()
    }

    fun skipToPreviousSong(){
        musicServiceConnection.transportControls.skipToPrevious()
    }

    fun seekTo(pos :Long){
        musicServiceConnection.transportControls.seekTo(pos)
    }


    fun playOrToggleSong(mediaItem: Song,toggle:Boolean =false){

        val isPrepared =playbackState.value?.isPrepared?:false
        if(isPrepared && mediaItem.mediaId ==
            curPlayingSong.value?.getString(METADATA_KEY_MEDIA_ID)){
            playbackState.value?.let {playbackState ->
                when{
                    playbackState.isPlaying-> if (toggle)musicServiceConnection.transportControls.pause()
                    playbackState.isPlayEnabled->musicServiceConnection.transportControls.play()
                    else  -> Unit
                }

            }
        }else{
            musicServiceConnection.transportControls.playFromMediaId(mediaItem.mediaId,null)
        }
    }


    override fun onCleared() {
        super.onCleared()
        musicServiceConnection.subscribe(MEDIA_ROOT_ID,object :MediaBrowserCompat.SubscriptionCallback(){})
    }

}