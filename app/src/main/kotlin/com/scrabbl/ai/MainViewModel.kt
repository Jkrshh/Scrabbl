package com.scrabbl.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scrabbl.ai.dict.DictState
import com.scrabbl.ai.dict.DictionaryRepository
import com.scrabbl.ai.engine.Board
import com.scrabbl.ai.engine.BoardType
import com.scrabbl.ai.engine.Dictionary
import com.scrabbl.ai.engine.FrenchScrabble
import com.scrabbl.ai.engine.Move
import com.scrabbl.ai.engine.MoveGenerator
import com.scrabbl.ai.vision.BoardDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(
    private val dictRepo: DictionaryRepository,
) : ViewModel() {

    val dictState: StateFlow<DictState> = dictRepo.state

    private val _board = MutableStateFlow(Board(BoardType.CLASSIC))
    val board: StateFlow<Board> = _board.asStateFlow()

    private val _rack = MutableStateFlow("")
    val rack: StateFlow<String> = _rack.asStateFlow()

    private val _moves = MutableStateFlow<List<Move>>(emptyList())
    val moves: StateFlow<List<Move>> = _moves.asStateFlow()

    private val _computing = MutableStateFlow(false)
    val computing: StateFlow<Boolean> = _computing.asStateFlow()

    private val _lastDetection = MutableStateFlow<BoardDetector.Detection?>(null)
    val lastDetection: StateFlow<BoardDetector.Detection?> = _lastDetection.asStateFlow()

    private val _boardLocked = MutableStateFlow(false)
    val boardLocked: StateFlow<Boolean> = _boardLocked.asStateFlow()

    private var computeJob: Job? = null

    init {
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                _board, _rack, dictRepo.dictionary,
            ) { b, r, d -> Triple(b, r, d) }.collect { (b, r, d) ->
                recompute(b, r, d)
            }
        }
    }

    fun setRack(text: String) {
        val cleaned = buildString(text.length) {
            for (c in text) {
                val n = FrenchScrabble.normalize(c)
                if (n in 'A'..'Z') append(n)
                else if (c == '?' || c == '*') append('?')
                if (length >= FrenchScrabble.RACK_SIZE) break
            }
        }
        _rack.value = cleaned
    }

    fun onDetection(det: BoardDetector.Detection) {
        _lastDetection.value = det
        if (!_boardLocked.value) _board.value = det.board.copy()
    }

    fun lockBoard(lock: Boolean) { _boardLocked.value = lock }

    fun setCellManual(row: Int, col: Int, letter: Char?) {
        val b = _board.value.copy()
        if (letter == null) {
            b.letters[row][col] = 0
            b.blanks[row][col] = false
        } else {
            val ch = FrenchScrabble.normalize(letter)
            if (ch in 'A'..'Z') b.letters[row][col] = FrenchScrabble.l(ch)
        }
        _board.value = b
    }

    fun setBoardType(type: BoardType) {
        if (_board.value.type != type) _board.value = Board(type)
    }

    fun clearBoard() {
        _board.value = Board(_board.value.type)
    }

    fun retryDictionaryDownload() {
        dictRepo.retryDownload()
    }

    private fun recompute(board: Board, rackText: String, dict: Dictionary) {
        computeJob?.cancel()
        if (rackText.isEmpty() || dict.size == 0) {
            _moves.value = emptyList(); return
        }
        computeJob = viewModelScope.launch {
            _computing.value = true
            val rack = IntArray(rackText.length) {
                val c = rackText[it]
                if (c == '?') 0 else FrenchScrabble.l(c)
            }
            val list = withContext(Dispatchers.Default) {
                MoveGenerator(dict).generate(board, rack, topK = 100)
            }
            _moves.value = list
            _computing.value = false
        }
    }
}
