package com.base.spotifymvvm.exoplayer

import android.media.MediaMetadata.*
import android.net.Uri
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import com.base.spotifymvvm.data.remote.MusicDatabase
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import androidx.core.net.toUri as toUri1

class FirebaseMusicSource @Inject constructor(
    private val musicDatabase: MusicDatabase
){


    var songs= emptyList<MediaMetadataCompat>()

    suspend fun fetchMediaData() = withContext(Dispatchers.IO){
     state= State.STATE_INITIALIZING
        val allSongs=musicDatabase.getAllSongs()
        songs=allSongs.map { song ->
            MediaMetadataCompat.Builder()
                .putString(METADATA_KEY_ARTIST,song.title)
                .putString(METADATA_KEY_MEDIA_ID,song.mediaId)
                .putString(METADATA_KEY_TITLE,song.title)
                .putString(METADATA_KEY_DISPLAY_TITLE,song.title)
                .putString(METADATA_KEY_DISPLAY_ICON_URI,song.imageUrl)
                .putString(METADATA_KEY_MEDIA_URI,song.songUrl)
                .putString(METADATA_KEY_ALBUM_ART_URI,song.imageUrl)
                .putString(METADATA_KEY_DISPLAY_SUBTITLE,song.subtitle)
                .putString(METADATA_KEY_DISPLAY_DESCRIPTION,song.subtitle)
                .build()
        }
state= State.STATE_INITIALIZED
    }

    fun asMediaSource(dataSourceFactory:DefaultDataSourceFactory):ConcatenatingMediaSource{
        val concatenatingMediaSource =ConcatenatingMediaSource()

        songs.forEach { song->
          val mediaSource=ProgressiveMediaSource.Factory(dataSourceFactory)

              .createMediaSource(song.getString(METADATA_KEY_MEDIA_URI).toUri1())
        concatenatingMediaSource.addMediaSource(mediaSource)
        }
        return concatenatingMediaSource

    }


    fun asMediaItems()=songs.map {song->
        val desc= MediaDescriptionCompat.Builder()
            .setMediaUri(song.getString(METADATA_KEY_MEDIA_URI).toUri1())
            .setTitle(song.description.title)
            .setSubtitle(song.description.subtitle)
            .setMediaId(song.description.mediaId)
            .setIconUri(song.description.iconUri)
            .build()
        MediaBrowserCompat.MediaItem(desc,FLAG_PLAYABLE)

    }.toMutableList()

    private val onReadyLiseners = mutableListOf<(Boolean)->Unit>()
    private var state:State = State.STATE_CREATED
    set(value){
        if(value==State.STATE_INITIALIZED ||value == State.STATE_ERROR){

            synchronized(onReadyLiseners){
                field=value
                onReadyLiseners.forEach { listener->
                    listener(state== State.STATE_INITIALIZED)

                }
            }
        }else {
            field=value
        }

    }
    fun whenReady(action :(Boolean)->Unit):Boolean{
        if (state== State.STATE_CREATED ||state== State.STATE_INITIALIZING){
            onReadyLiseners+=action
            return false
        }else{
            action(state == State.STATE_INITIALIZED)
            return true
        }
    }
}


enum class State{


    STATE_CREATED,
    STATE_INITIALIZING,
    STATE_INITIALIZED,
    STATE_ERROR

}