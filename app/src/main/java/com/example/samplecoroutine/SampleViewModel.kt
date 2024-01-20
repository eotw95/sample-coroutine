package com.example.samplecoroutine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SampleViewModel:ViewModel() {
    fun asyncTask() {
        println("start SampleViewModel async task")
        // viewModelScope.launch{}は非同期的に動くので、処理の完了を待たずに後続の処理を実行する
        viewModelScope.launch {
            repeat(3) {
                withContext(Dispatchers.IO) {
                    println("$it thread name: ${Thread.currentThread().name}")
                    println("start thread number: $it")
                    delay(1000)
                    println("end thread number: $it")
                }
            }
        }
        println("end SampleViewModel async task")
    }
}