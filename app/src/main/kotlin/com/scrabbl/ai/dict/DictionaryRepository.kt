package com.scrabbl.ai.dict

import android.content.Context
import com.scrabbl.ai.engine.Dictionary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.GZIPInputStream

class DictionaryRepository(
    private val appContext: Context,
    private val downloader: DictionaryDownloader = DictionaryDownloader(),
) {
    private val _state = MutableStateFlow<DictState>(DictState.Idle)
    val state: StateFlow<DictState> = _state.asStateFlow()

    private val _dictionary = MutableStateFlow(Dictionary.EMPTY)
    val dictionary: StateFlow<Dictionary> = _dictionary.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun load() = withContext(Dispatchers.IO) {
        _state.value = DictState.LoadingStarter
        val starter = loadStarter()
        _dictionary.value = starter
        _state.value = DictState.StarterReady(starter.size)

        val cache = cacheFile()
        if (cache.exists() && cache.length() > 100_000) {
            runCatching { loadFrom(cache) }.getOrNull()?.let { full ->
                _dictionary.value = full
                _state.value = DictState.FullReady(full.size)
                return@withContext
            }
        }

        doDownload()
    }

    fun retryDownload() {
        scope.launch { doDownload() }
    }

    private suspend fun doDownload() {
        _state.value = DictState.Downloading(0)
        try {
            val bytes = downloader.download { pct -> _state.value = DictState.Downloading(pct) }
            val cache = cacheFile()
            cache.parentFile?.mkdirs()
            cache.writeBytes(bytes)
            val full = loadFrom(cache)
            _dictionary.value = full
            _state.value = DictState.FullReady(full.size)
        } catch (t: Throwable) {
            _state.value = DictState.Failed(t.message ?: "erreur inconnue")
        }
    }

    private fun cacheFile(): File = File(appContext.filesDir, "dict/ods_fr.txt")

    private fun loadStarter(): Dictionary {
        val words = ArrayList<String>(4_096)
        appContext.assets.open("starter_fr.txt").bufferedReader().use { br ->
            br.lineSequence().forEach { line ->
                val t = line.trim()
                if (t.isNotEmpty() && !t.startsWith("#")) words.add(t)
            }
        }
        return Dictionary.from(words)
    }

    private fun loadFrom(file: File): Dictionary {
        val words = ArrayList<String>(500_000)
        val stream = if (file.name.endsWith(".gz")) {
            GZIPInputStream(file.inputStream())
        } else file.inputStream()
        stream.bufferedReader().use { br ->
            br.lineSequence().forEach { line ->
                val t = line.trim()
                if (t.isNotEmpty() && !t.startsWith("#")) words.add(t)
            }
        }
        return Dictionary.from(words)
    }
}

sealed class DictState {
    object Idle : DictState()
    object LoadingStarter : DictState()
    data class StarterReady(val words: Int) : DictState()
    data class Downloading(val percent: Int) : DictState()
    data class FullReady(val words: Int) : DictState()
    data class Failed(val message: String) : DictState()
}
