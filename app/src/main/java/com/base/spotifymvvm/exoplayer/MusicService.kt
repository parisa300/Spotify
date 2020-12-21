package com.base.spotifymvvm.exoplayer

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat
import com.base.spotifymvvm.exoplayer.callbacks.MusicPlaybackPreparer
import com.base.spotifymvvm.exoplayer.callbacks.MusicPlayerEventListener
import com.base.spotifymvvm.exoplayer.callbacks.MusicPlayerNotificationListener
import com.base.spotifymvvm.other.Constants.MEDIA_ROOT_ID
import com.base.spotifymvvm.other.Constants.NETWORK_ERROR
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.scopes.ServiceScoped
import kotlinx.coroutines.*
import java.time.Duration
import javax.inject.Inject

private const val SERVICE_TAG="MusicService"

@AndroidEntryPoint
class MusicService  :MediaBrowserServiceCompat(){

    @Inject
    lateinit var dataSourceFactory  : DefaultDataSourceFactory
    @Inject
    lateinit var exoplayer :SimpleExoPlayer

    @Inject
    lateinit var firebaseMusicSource: FirebaseMusicSource
     private lateinit var musicNotificationManager: MusicNotificationManager
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main+serviceJob)
    private lateinit var mediaSession :MediaSessionCompat
    private lateinit var mediaSessionConnector: MediaSessionConnector

   var isForegroundService=false

    private var curPlayingSong :MediaMetadataCompat?=null
    private var isPlayerInitialized =false

    private lateinit var musicPlayerEventListener: MusicPlayerEventListener

    companion object{

        var curSongDuration= 0L
        private set
    }

    override fun onCreate() {
        super.onCreate()

        serviceScope.launch {
            firebaseMusicSource.fetchMediaData()
        }
        val activityIntent =packageManager?.getLeanbackLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this,0,it,0)
        }
        mediaSession= MediaSessionCompat(this,SERVICE_TAG).apply {
            setSessionActivity(activityIntent)
            isActive=true
        }
        sessionToken=mediaSession.sessionToken


        musicNotificationManager= MusicNotificationManager(
            this,
            mediaSession.sessionToken,
           MusicPlayerNotificationListener(this)

        ){
         curSongDuration=exoplayer.duration
        }

        val musicPlayerPreparer = MusicPlaybackPreparer(firebaseMusicSource){
            curPlayingSong =it

            preparePlayer(
                firebaseMusicSource.songs,
                it,
                true

            )
        }

        mediaSessionConnector= MediaSessionConnector(mediaSession)
        mediaSessionConnector.setPlaybackPreparer(musicPlayerPreparer)
        mediaSessionConnector.setQueueNavigator(MusicQueueNavigator())
        mediaSessionConnector.setPlayer(exoplayer)
        musicPlayerEventListener=MusicPlayerEventListener(this)
        exoplayer.addListener(musicPlayerEventListener)
        musicNotificationManager.showNotification(exoplayer)
    }

    private inner class MusicQueueNavigator :TimelineQueueNavigator(mediaSession){
        override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {
           return firebaseMusicSource.songs[windowIndex].description
        }


    }




    private fun preparePlayer(
        songs:List<MediaMetadataCompat>,
        itemToPlay :MediaMetadataCompat?,
        playNow :Boolean
    ){
        val curSongIndex =if (curPlayingSong==null) 0 else songs.indexOf(itemToPlay)
        exoplayer.prepare(firebaseMusicSource.asMediaSource(dataSourceFactory))
        exoplayer.seekTo(curSongIndex,0L)
        exoplayer.playWhenReady =playNow
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        exoplayer.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        exoplayer.removeListener(musicPlayerEventListener)
        exoplayer.release()
    }
    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        when(parentId){

            MEDIA_ROOT_ID->{
                val resultSent =firebaseMusicSource.whenReady {isInitialized->
                    if (isInitialized){
                        result.sendResult(firebaseMusicSource.asMediaItems())
                        if(!isPlayerInitialized && firebaseMusicSource.songs.isNotEmpty()){
                            preparePlayer(firebaseMusicSource.songs,firebaseMusicSource.songs[0],false)
                            isPlayerInitialized=true
                        }
                    }else{
                        mediaSession.sendSessionEvent(NETWORK_ERROR,null)
                        result.sendResult(null)
                    }

                }
                if (!resultSent){
                    result.detach()
                }
            }
        }
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
       return BrowserRoot(MEDIA_ROOT_ID,null)
    }


}