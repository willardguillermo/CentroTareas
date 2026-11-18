package com.willard.centrotareas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.willard.centrotareas.data.TareasRepositoryLocal
import com.willard.centrotareas.ui.PantallaTareas
import com.willard.centrotareas.ui.theme.CentroTareasTheme
import com.willard.centrotareas.viewmodel.TareasViewModel
import com.willard.centrotareas.viewmodel.TareasViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CentroTareasTheme {
                val vm: TareasViewModel = viewModel(
                    factory = TareasViewModelFactory(TareasRepositoryLocal())
                )
                PantallaTareas(vm)
            }
        }
    }
}