package com.base.spotifymvvm.ui.fragments

import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import android.view.View
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import com.bumptech.glide.RequestManager
import com.google.android.exoplayer2.C
import com.base.spotifymvvm.R
import com.base.spotifymvvm.data.entitie.Song
import com.base.spotifymvvm.exoplayer.isPlaying
import com.base.spotifymvvm.exoplayer.toSong
import com.base.spotifymvvm.other.Status
import com.base.spotifymvvm.other.Status.*
import com.base.spotifymvvm.ui.viewmodel.MainViewModel
import com.base.spotifymvvm.ui.viewmodel.SongViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_song.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class SongFragment :Fragment(R.layout.fragment_song) {

@Inject
lateinit var glide :RequestManager

    private lateinit var mainViewModel: MainViewModel
    private val songViewModel :SongViewModel by viewModels ()

    private var curPlayingSong :Song? =null
    private var playbackState :PlaybackStateCompat?=null
    private var shouldUpdatetoSeekbar =true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainViewModel=ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
        subscribeToObservers()

        ivPlayPauseDetail.setOnClickListener{
            curPlayingSong?.let {
                mainViewModel.playOrToggleSong(it,true)
            }
        }



        seekBar.setOnSeekBarChangeListener(object :SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            if (fromUser){
            setCurPlayerTimeToTextview(progress.toLong())
            }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
                shouldUpdatetoSeekbar=false
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
              seekBar?.let{
                  mainViewModel.seekTo(it.progress.toLong())
                  shouldUpdatetoSeekbar=true
              }
            }


        })

        ivSkipPrevious.setOnClickListener{
            mainViewModel.skipToPreviousSong()
        }

       ivSkip.setOnClickListener {
           mainViewModel.skipToNextSong()
       }
    }

    private fun updateTitleAndSongImage(song: Song){
        val title = "${song.title}-${song.subtitle}"
        tvSongName.text=title
        glide.load(song.imageUrl).into(ivSongImage)
    }

    private fun subscribeToObservers(){
        mainViewModel.mediaItems.observe(viewLifecycleOwner){
            it?.let {result ->
                when(result.status){
                    SUCCESS ->{
                     result.data?.let {songs->
                         if (curPlayingSong ==null && songs.isNotEmpty()){
                             curPlayingSong=songs[0]
                             updateTitleAndSongImage(songs[0])

                         }


                     }

                    }else ->Unit
                }

            }
        }

        mainViewModel.curPlayingSong.observe(viewLifecycleOwner){
            if (it ==null)return@observe
            curPlayingSong=it.toSong()
            updateTitleAndSongImage(curPlayingSong!!)
        }

        mainViewModel.playbackState.observe(viewLifecycleOwner){
            playbackState=it
            ivPlayPauseDetail.setImageResource(
                if (playbackState?.isPlaying==true)R.drawable.ic_pause else R.drawable.ic_play
            )

            seekBar.progress=it?.position?.toInt() ?:0
        }

        songViewModel.curPlayerPosition.observe(viewLifecycleOwner){

            if (shouldUpdatetoSeekbar){
                seekBar.progress=it.toInt()
                setCurPlayerTimeToTextview(it)
            }
        }

        songViewModel.curSongDuration.observe(viewLifecycleOwner){
            seekBar.max=it.toInt()
            val dataFormat = SimpleDateFormat("mm:ss", Locale.getDefault())
          //  tvSongDuration.text=dataFormat.format(it)
            tvSongDuration.text = dataFormat.format(it-(60*1000*30))
        }
    }
  private fun setCurPlayerTimeToTextview(ms :Long){
      val dataFormat = SimpleDateFormat("mm:ss", Locale.getDefault())
     // tvCurTime.text=dataFormat.format(ms)
      tvCurTime.text = dataFormat.format(ms -(60*1000*30))



  }

}