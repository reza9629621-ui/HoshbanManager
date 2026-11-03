package com.example.hoshban

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hoshban.data.LinkRepository
import com.example.hoshban.ui.MainViewModel
import com.example.hoshban.ui.screens.MainScreen
import com.example.hoshban.ui.theme.HoshbanTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val repository = LinkRepository(applicationContext)
        setContent {
            HoshbanTheme {
                val vm: MainViewModel = viewModel(factory = MainViewModel.Factory(repository))
                val links by vm.links.collectAsState()
                val snackbar by vm.snackbar.collectAsState()
                
                MainScreen(
                    links = links,
                    snackbar = snackbar,
                    onAdd = vm::addLink,
                    onUpdate = vm::updateLink,
                    onDelete = vm::deleteLink,
                    onExecute = vm::execute,
                    onSnackbarShown = vm::clearSnackbar
                )
            }
        }
    }
}