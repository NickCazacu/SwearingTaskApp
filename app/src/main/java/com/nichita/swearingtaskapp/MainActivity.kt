package com.nichita.swearingtaskapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.nichita.swearingtaskapp.ui.navigation.AppNavHost
import com.nichita.swearingtaskapp.ui.theme.RoastTodoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RoastTodoTheme {
                AppNavHost()
            }
        }
    }
}
