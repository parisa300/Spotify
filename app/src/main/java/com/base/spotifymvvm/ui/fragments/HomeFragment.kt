package com.base.spotifymvvm.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import com.base.spotifymvvm.R
import com.base.spotifymvvm.adapter.SongAdapter
import com.base.spotifymvvm.other.Status
import com.base.spotifymvvm.ui.viewmodel.MainViewModel

import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_home.*
import javax.inject.Inject
import javax.net.ssl.SSLEngineResult

@AndroidEntryPoint
class HomeFragment :Fragment(R.layout.fragment_home) {

    lateinit var mainViewModel : MainViewModel

    @Inject
    lateinit var songAdapter: SongAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainViewModel=ViewModelProvider(requireActivity()).get(MainViewModel::class.java)

        setupRecyclerView()
        subscribeToObservers()
        songAdapter.setClickListener {
            mainViewModel.playOrToggleSong(it)
        }
    }


    private fun setupRecyclerView()=rvAllSongs.apply {
        adapter = songAdapter
        layoutManager=LinearLayoutManager(requireContext())
    }
    private fun subscribeToObservers(){
        mainViewModel.mediaItems.observe(viewLifecycleOwner){result ->
            when(result.status){
                Status.SUCCESS -> {

                    allSongsProgressBar.isVisible=false
                    result.data?.let { songs->
                        songAdapter.songs = songs
                    }
                }
                Status.ERROR ->Unit
                Status.LOADING ->allSongsProgressBar.isVisible = true
            }

        }

    }
}