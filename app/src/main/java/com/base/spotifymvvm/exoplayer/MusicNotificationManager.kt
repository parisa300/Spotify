package com.base.spotifymvvm.exoplayer

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import com.base.spotifymvvm.R
import com.base.spotifymvvm.other.Constants.NOTIFICATION_CHANEL_ID
import com.base.spotifymvvm.other.Constants.NOTIFICATION_ID
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager


class MusicNotificationManager(
    private val context: Context,
    sessionToken :MediaSessionCompat.Token,
    notificationListener: PlayerNotificationManager.NotificationListener,
    private val newSongCallback :()->Unit
) {

    private val notificationManager :PlayerNotificationManager
    init {
        val mediaController=MediaControllerCompat(context,sessionToken)
        notificationManager= PlayerNotificationManager.createWithNotificationChannel(

            context,
            NOTIFICATION_CHANEL_ID,
            R.string.notification_chanel_name,
            R.string.notification_chanel_description,
            NOTIFICATION_ID,
            DescriptionAdapter(mediaController),
         notificationListener

        ).apply {
            setSmallIcon(R.drawable.ic_music)
            setMediaSessionToken(sessionToken)
        }
    }


    fun showNotification(player: Player){
        notificationManager.setPlayer(player)
    }

    private inner class DescriptionAdapter(
        private val mediaController:MediaControllerCompat

    ):PlayerNotificationManager.MediaDescriptionAdapter{
        override fun createCurrentContentIntent(player: Player): PendingIntent? {

         return mediaController.sessionActivity
        }

        override fun getCurrentContentText(player: Player): CharSequence? {
          return mediaController.metadata.description.subtitle.toString()
        }

        override fun getCurrentContentTitle(player: Player): CharSequence {
            newSongCallback()
            return mediaController.metadata.description.title.toString()
        }

        override fun getCurrentLargeIcon(
            player: Player,
            callback: PlayerNotificationManager.BitmapCallback
        ): Bitmap? {
           Glide.with(context).asBitmap()
               .load(mediaController.metadata.description.iconUri)
               .into(object :CustomTarget<Bitmap>(){
                   override fun onLoadCleared(placeholder: Drawable?) =Unit


                   override fun onResourceReady(
                       resource: Bitmap,
                       transition: Transition<in Bitmap>?
                   ) {
                       callback.onBitmap(resource)
                   }


               })
            return null
        }

    }
}