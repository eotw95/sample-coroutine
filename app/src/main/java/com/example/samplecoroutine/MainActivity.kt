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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
                    // first async task
                    asyncTask()

                    // second async task
                    viewModel.asyncTask()
                }
            }
        }
    }

    private fun asyncTask() {
        println("start MainActivity async task")
        val mutex = Mutex()
        // runBlocking{}は、同期的に動くので、後続の処理はrunBlocking{}が終了するのを待つ
        runBlocking {
            repeat(3) {
                launch(Dispatchers.IO) {
                    mutex.withLock {
                        println("$it thread name: ${Thread.currentThread().name}")
                        println("start thread number: $it")
                        delay(1000)
                        println("end thread number: $it")
                    }
                }
            }
        }
        println("end MainActivity async task")
    }
}