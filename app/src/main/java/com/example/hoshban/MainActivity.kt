package com.example.hoshban

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hoshban.data.LinkConfig
import com.example.hoshban.data.LinkRepository
import com.example.hoshban.ui.MainViewModel
import com.example.hoshban.ui.theme.HoshbanTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HoshbanTheme {
                val repository = LinkRepository(applicationContext)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    links: List<LinkConfig>,
    snackbar: String?,
    onAdd: (LinkConfig) -> Unit,
    onUpdate: (LinkConfig) -> Unit,
    onDelete: (String) -> Unit,
    onExecute: (LinkConfig) -> Unit,
    onSnackbarShown: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { Text("Hoshban Manager V2", modifier = Modifier.padding(16.dp)) }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            Text("برنامه در حال آماده‌سازی است...")
        }
    }
}
