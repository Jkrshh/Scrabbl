package com.scrabbl.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.scrabbl.ai.ui.MainScreen
import com.scrabbl.ai.ui.theme.ScrabblTheme

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(ScrabblApp.get().dictRepo) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScrabblTheme {
                MainScreen(vm)
            }
        }
    }
}
