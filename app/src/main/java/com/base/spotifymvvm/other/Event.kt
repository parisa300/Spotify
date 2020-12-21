package com.base.spotifymvvm.other

 open class Event<out T>(private  val data :T) {

     var hasBeanHandled =false
     private set

     fun getContentIfNotHandeled():T?{
         return if (hasBeanHandled){
             null
         }else{
             hasBeanHandled=true
             data
         }
     }
     fun peekContent()=data
}