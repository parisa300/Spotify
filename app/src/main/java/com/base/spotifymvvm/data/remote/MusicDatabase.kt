package com.base.spotifymvvm.data.remote

import com.base.spotifymvvm.data.entitie.Song
import com.base.spotifymvvm.other.Constants.SONG_COLLECTION
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

import kotlinx.coroutines.tasks.await

class MusicDatabase {
    private val firestore=FirebaseFirestore.getInstance()

    private val songcollectin =firestore.collection(SONG_COLLECTION)

    suspend fun getAllSongs():List<Song>{

      return  try {
          songcollectin.get().await().toObjects(Song ::class.java)

        }catch (e:Exception){
          emptyList()
      }
    }
}