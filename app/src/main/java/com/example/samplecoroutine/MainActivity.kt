package com.example.samplecoroutine

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.samplecoroutine.ui.theme.SampleCoroutineTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.EmptyCoroutineContext

class MainActivity : ComponentActivity() {
    val viewModel = SampleViewModel()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SampleCoroutineTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    asyncTask()
//                    syncTask()
                }
            }
        }
    }

    private fun asyncTask() {
        println("start async task")
        runBlocking {
            val deferred1 = async {
                println("async 1")
                1
            }
            val deferred2 = async {
                println("async 2")
                1
            }

            val value1 = deferred1.await()
            val value2 = deferred2.await()

            println("finish")
            println("result=${value1 + value2}")
        }
    }
    private fun asyncTask2() {
        // CoroutineContextには、Coroutineを実行するスレッド(Dispatchers.Default)の種類の情報を持たせることができる
        val context1 = EmptyCoroutineContext
        val scope1 = CoroutineScope(context1)
        scope1.launch {
            // EmptyCoroutineContextはデフォルトのスレッド(バックグランドスレッド)を指定する
        }

        // Dispatchers.Main
        val context2 = Dispatchers.Main
        val scope2 = CoroutineScope(context2)
        scope2.launch {
            // Dispatchers.MainなのでUIスレッドで実行
        }

        // JobもCoroutineContextの1種で、併用できる。Dispatchers.Main + Job
        val context3 = Dispatchers.Main + Job()
        val scope3 = CoroutineScope(context3)
        scope3.launch {
            // Dispatchers.MainなのでUIスレッドで実行
            println("start")
            delay(1000)
            println("finish")
        }
        // context3を利用しているCoroutineScopeは全てキャンセルされる
        context3.cancel()
    }
    private fun syncTask() {
        println("start sync task")
        repeat(50) {
            runBlocking {
                println("■■■■■■■■■■■■")
                launch {
                    println("step 1")
                    Thread.sleep(500)
                    println("step 2")
                }
                launch {
                    println("step 3")
                }
                println("■■■■■■■■■■■■")
            }
        }
    }
}