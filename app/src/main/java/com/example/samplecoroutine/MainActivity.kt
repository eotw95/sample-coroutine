package com.example.samplecoroutine

import android.os.Bundle
import android.provider.ContactsContract.Data
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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
                    asyncTask5()
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
    private fun asyncTask3() {
        val exception = CoroutineExceptionHandler{ _, e ->
            println("exception: $e")
        }
        val context = SupervisorJob() + exception
        val parentScope = CoroutineScope(context)
        parentScope.launch {
            // thisはlaunch{}によって新たに作成された子CoroutineScopeでSupervisorJob()ではなく、Job()インスタンスを持っている
            // → Job()なので、片方のCoroutineでExceptionをCatchした瞬間に、もう片方のCoroutineもキャンセルされる
            this.launch {
                println("step 1")
                delay(500)
                throw Exception("error")
            }
            this.launch {
                println("step2")
                delay(1000)
                println("finish step2")
            }
        }
    }
    private fun asyncTask4() {
        val exception = CoroutineExceptionHandler { _, e ->
            println("catch by exception handler : $e")
        }
        val context = Job() + exception
        val scope = CoroutineScope(context)
        scope.launch {
            val deferred = async { throw Exception("error") }
            try { deferred.await() } catch (e: Exception) { println("catch exception by await") }
        }
    }

    private fun asyncTask5() {
        val scope = CoroutineScope(EmptyCoroutineContext)
        scope.launch {
            try {
                // suspend関数の処理がtimeoutしたらTimeoutCancellationExceptionをスローする
                withTimeout(100L) { suspendTask() }
            } catch(e: Exception) { e.printStackTrace() }
            // suspend関数の処理がtimeoutしたらnullを返す
            withTimeoutOrNull(100L) { suspendTask() } ?: println("Timeout")
        }
    }
    private suspend fun suspendTask() {
        println("suspendTask")
        delay(1000L)
    }
    private fun asyncTask6() {
        val scope = CoroutineScope(EmptyCoroutineContext)
        scope.launch {
            retryOrNull(10, 100L) { suspendTask() } ?: println("何かしらのデフォルト実装")
        }
    }
    suspend fun fetchDataSuspend(): Data {
        return suspendCancellableCoroutine {
            val task = fetchDataCallback(object : onDataListener<Data> {
                override fun onSuccess(data: Data) {
                    it.resume(data)
                }

                override fun onFailure(e: Throwable) {
                    it.resumeWithException(e)
                }
            })
            it.invokeOnCancellation { task.cancel() }
        }
    }
    interface onDataListener<T> {
        fun onSuccess(data: T)
        fun onFailure(e: Throwable)
    }
    class Task() {
        fun cancel() {}
    }
    private fun fetchDataCallback(onDataListener: onDataListener<Data>): Task { return Task() }
    private suspend fun <T>retryOrNull(
        retry: Int,
        intervalMills: Long,
        predicate: suspend (Throwable) -> Boolean = { true }, // predicateはプログラミングの文脈で、引数を通して何かしらの判定処理を表す関数
        block: suspend () -> T
    ): T? {
        repeat(retry) {
            try {
                return block()
            } catch (e: CancellationException) {
                // coroutineをキャンセルしたい場合は、再throwしないとException発生しない
                throw e
            } catch (e: Throwable) {
                // predicateでfalseの場合はnull返して終了させる、trueの場合はrepeatする
                if (!predicate(e)) return null else delay(intervalMills)
            }
        }
        return null
    }

    // while{}内でscope.cancel()を実装してもcancelされずにループするので、ensureActive()でcoroutineの状態をチェックする
    private fun asyncTask7() {
        val scope = CoroutineScope(EmptyCoroutineContext)
        scope.launch {
            var i = 0
            while (i <= 1_000_000) {
                if (i % 1_000 == 0) println("$i")
                if (i == 5_000) scope.cancel()
                i ++
                ensureActive() // isActiveがfalseならCancellationExceptionをthrowする
            }
        }
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