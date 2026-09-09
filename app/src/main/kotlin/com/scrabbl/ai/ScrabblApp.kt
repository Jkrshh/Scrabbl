package com.scrabbl.ai

import android.app.Application
import com.scrabbl.ai.dict.DictionaryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ScrabblApp : Application() {

    lateinit var dictRepo: DictionaryRepository
        private set

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
        dictRepo = DictionaryRepository(applicationContext)
        appScope.launch { dictRepo.load() }
    }

    companion object {
        private lateinit var INSTANCE: ScrabblApp
        fun get(): ScrabblApp = INSTANCE
    }
}
