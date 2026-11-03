package com.example.hoshban

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

// ------------------ Data ------------------
@Serializable
data class LinkConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val codeSystem: String,
    val email: String
)

val Context.dataStore by preferencesDataStore(name = "hoshban_prefs")
val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

// ------------------ Network ------------------
suspend fun executeLink(config: LinkConfig): Boolean = withContext(Dispatchers.IO) {
    try {
        val urlString = "https://hoshban.ir/gps/user/save_session.php?Code_system=${config.codeSystem}&email=${config.email}"
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        val responseCode = connection.responseCode
        connection.disconnect()
        responseCode in 200..299
    } catch (e: Exception) {
        false
    }
}

// ------------------ Main Activity ------------------
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF1E6F5C),
                    onPrimary = Color.White,
                    background = Color(0xFFF4F7F6)
                )
            ) {
                MainScreen()
            }
        }
    }
}

// ------------------ UI ------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var links by remember { mutableStateOf<List<LinkConfig>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }
    var editingLink by remember { mutableStateOf<LinkConfig?>(null) }

    // خواندن اطلاعات از دیتابیس
    LaunchedEffect(Unit) {
        context.dataStore.data.map { prefs ->
            val raw = prefs[stringPreferencesKey("saved_links")] ?: "[]"
            try { json.decodeFromString<List<LinkConfig>>(raw) } catch (e: Exception) { emptyList() }
        }.collect { links = it }
    }

    // ذخیره در دیتابیس
    fun saveLinks(newLinks: List<LinkConfig>) {
        links = newLinks
        scope.launch {
            context.dataStore.edit { it[stringPreferencesKey("saved_links")] = json.encodeToString(newLinks) }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xFF0F4C3A), Color(0xFF4CAF8F)))).statusBarsPadding().padding(24.dp)) {
                Column {
                    Text("Hoshban Manager V2", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    Text("نسخه اصلاح شده", color = Color.White.copy(0.8f), fontSize = 16.sp)
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editingLink = null; showDialog = true },
                containerColor = Color(0xFF1E6F5C), contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("افزودن لینک")
            }
        }
    ) { padding ->
        if (links.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("هنوز لینکی اضافه نکرده‌اید")
            }
        } else {
            LazyColumn(Modifier.padding(padding).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(links, key = { it.id }) { link ->
                    Card(shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(6.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(link.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Code: ${link.codeSystem}", fontSize = 13.sp, color = Color.Gray)
                            Text("Email: ${link.email}", fontSize = 13.sp, color = Color.Gray)
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    scope.launch {
                                        val ok = executeLink(link)
                                        snackbarHostState.showSnackbar(if (ok) "✅ اجرا شد" else "❌ خطا در اتصال")
                                    }
                                }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E6F5C))) {
                                    Text("اجرا")
                                }
                                IconButton(onClick = { editingLink = link; showDialog = true }) {
                                    Icon(Icons.Default.Edit, null, tint = Color(0xFF1E6F5C))
                                }
                                IconButton(onClick = { saveLinks(links.filterNot { it.id == link.id }) }) {
                                    Icon(Icons.Default.Delete, null, tint = Color.Red)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AddEditDialog(
            link = editingLink,
            onDismiss = { showDialog = false },
            onSave = { newLink ->
                if (editingLink == null) saveLinks(links + newLink)
                else saveLinks(links.map { if (it.id == newLink.id) newLink else it })
                showDialog = false
            }
        )
    }
}

@Composable
fun AddEditDialog(link: LinkConfig?, onDismiss: () -> Unit, onSave: (LinkConfig) -> Unit) {
    var name by remember { mutableStateOf(link?.name ?: "") }
    var code by remember { mutableStateOf(link?.codeSystem ?: "") }
    var email by remember { mutableStateOf(link?.email ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (link == null) "افزودن لینک" else "ویرایش لینک") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("نام") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Code_system") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank() && code.isNotBlank() && email.isNotBlank()) {
                    onSave(LinkConfig(link?.id ?: UUID.randomUUID().toString(), name, code, email))
                }
            }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}