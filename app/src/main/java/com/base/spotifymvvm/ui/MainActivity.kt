package com.base.spotifymvvm.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.RequestManager
import com.google.android.material.snackbar.Snackbar
import com.base.spotifymvvm.R
import com.base.spotifymvvm.adapter.SwipeSongAdapter
import com.base.spotifymvvm.data.entitie.Song
import com.base.spotifymvvm.exoplayer.isPlaying
import com.base.spotifymvvm.exoplayer.toSong
import com.base.spotifymvvm.other.Status
import com.base.spotifymvvm.other.Status.*
import com.base.spotifymvvm.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Error
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
  private val mainViewModel :MainViewModel by viewModels()
    @Inject
    lateinit var glide :RequestManager


    @Inject
    lateinit var swipeSongAdapter: SwipeSongAdapter

    private var curPlayingSong : Song? =null

    private var playbackState :PlaybackStateCompat?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        subscribeToObserve()
        vpSong.adapter=swipeSongAdapter

        vpSong.registerOnPageChangeCallback(object :ViewPager2.OnPageChangeCallback(){

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (playbackState?.isPlaying==true){
                    mainViewModel.playOrToggleSong(swipeSongAdapter.songs[position])
                }else{
                    curPlayingSong=swipeSongAdapter.songs[position]

                }
            }
        })


        ivPlayPause.setOnClickListener {
            curPlayingSong?.let {
                mainViewModel.playOrToggleSong(it,true)
            }
        }

        swipeSongAdapter.setClickListener {
            navHostFragment.findNavController().navigate(R.id.globalActionToSongFragment)
        }

        navHostFragment.findNavController().addOnDestinationChangedListener { _, destination, _ ->

            when(destination.id){
                R.id.songFragment ->hideBottomBar()
                R.id.homeFragment->showBottomBar()
                else ->showBottomBar()
            }
        }
    }

    private fun hideBottomBar(){
        ivCurSongImage.isVisible=false
        vpSong.isVisible=false
        ivPlayPause.isVisible =false
    }

    private fun showBottomBar(){
        ivCurSongImage.isVisible=true
        vpSong.isVisible=true
        ivPlayPause.isVisible =true
    }

    private fun switchViewPagerToCurrentSong(song: Song){
        val newItemIndex=swipeSongAdapter.songs.indexOf(song)
        if (newItemIndex != -1){
            vpSong.currentItem =newItemIndex
            curPlayingSong=song
        }
    }

    private fun subscribeToObserve(){
        mainViewModel.mediaItems.observe(this){
            it?.let {result ->
                when(result.status){

                    SUCCESS ->{
                    result.data?.let {songs->

                    swipeSongAdapter.songs=songs

                        if (songs.isNotEmpty()){
                            glide.load((curPlayingSong?:songs[0]).imageUrl).into(ivCurSongImage)
                        }

                        switchViewPagerToCurrentSong(curPlayingSong?:return@observe)

                    }
                    }
                    ERROR->Unit
                    LOADING->Unit
                }

            }
        }

        mainViewModel.curPlayingSong.observe(this){
            if(it==null)return@observe
            curPlayingSong =it.toSong()
            glide.load(curPlayingSong?.imageUrl).into(ivCurSongImage)
            switchViewPagerToCurrentSong(curPlayingSong?:return@observe)

        }

        mainViewModel.playbackState.observe(this){
            playbackState=it

            ivPlayPause.setImageResource(
                if (playbackState?.isPlaying ==true)R.drawable.ic_pause else R.drawable.ic_play
            )
        }

        mainViewModel.isConnected.observe(this){
            it?.getContentIfNotHandeled()?.let { result->
                when(result.status){
                    ERROR ->Snackbar.make(
                        rootLayout,
                        result.message ?: "An Unknow Error",
                    Snackbar.LENGTH_LONG).show()
                  else ->Unit
                }
            }
        }

        mainViewModel.networkError.observe(this){
            it?.getContentIfNotHandeled()?.let { result->
                when(result.status){
                    ERROR ->Snackbar.make(
                        rootLayout,
                        result.message ?: "An Unknow Error",
                        Snackbar.LENGTH_LONG).show()
                    else ->Unit
                }
            }
        }
    }
}