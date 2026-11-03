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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

// ------------------ مدل داده‌ها ------------------
data class LinkConfig(
    val id: String,
    val name: String,
    val codeSystem: String,
    val email: String
)

// ------------------ توابع ذخیره و خواندن (SharedPreferences) ------------------
fun loadLinks(context: Context): List<LinkConfig> {
    val prefs = context.getSharedPreferences("hoshban_prefs", Context.MODE_PRIVATE)
    val jsonString = prefs.getString("saved_links", "[]") ?: "[]"
    val list = mutableListOf<LinkConfig>()
    try {
        val array = JSONArray(jsonString)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(LinkConfig(
                obj.getString("id"),
                obj.getString("name"),
                obj.getString("codeSystem"),
                obj.getString("email")
            ))
        }
    } catch (e: Exception) { }
    return list
}

fun saveLinks(context: Context, links: List<LinkConfig>) {
    val array = JSONArray()
    links.forEach {
        val obj = JSONObject()
        obj.put("id", it.id)
        obj.put("name", it.name)
        obj.put("codeSystem", it.codeSystem)
        obj.put("email", it.email)
        array.put(obj)
    }
    context.getSharedPreferences("hoshban_prefs", Context.MODE_PRIVATE)
        .edit()
        .putString("saved_links", array.toString())
        .apply()
}

// ------------------ تابع ارسال درخواست شبکه ------------------
suspend fun executeLink(config: LinkConfig): String = withContext(Dispatchers.IO) {
    try {
        val urlString = "https://hoshban.ir/gps/user/save_session.php?Code_system=${config.codeSystem}&email=${config.email}"
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.instanceFollowRedirects = true
        
        // معرفی برنامه به عنوان مرورگر برای جلوگیری از بلاک شدن توسط سرور
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")

        val responseCode = connection.responseCode
        connection.disconnect()
        
        if (responseCode in 200..299) {
            "✅ درخواست با موفقیت ارسال شد"
        } else {
            "❌ خطای سرور (کد $responseCode)"
        }
    } catch (e: Exception) {
        "❌ خطا در اتصال: ${e.message ?: "مشکل شبکه"}"
    }
}

// ------------------ رابط کاربری (Activity) ------------------
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var links by remember { mutableStateOf(loadLinks(context)) }
    var showDialog by remember { mutableStateOf(false) }
    var editingLink by remember { mutableStateOf<LinkConfig?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0xFF0F4C3A), Color(0xFF4CAF8F))))
                    .statusBarsPadding()
                    .padding(24.dp)
            ) {
                Column {
                    Text("Hoshban Manager V2", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    Text("نسخه اصلاح شده", color = Color.White.copy(0.8f), fontSize = 16.sp)
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editingLink = null; showDialog = true },
                containerColor = Color(0xFF1E6F5C),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("افزودن لینک")
            }
        }
    ) { padding ->
        if (links.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("هنوز لینکی اضافه نکرده‌اید", color = Color.Gray)
            }
        } else {
            LazyColumn(
                Modifier.padding(padding).fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(links, key = { it.id }) { link ->
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(6.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(link.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                            Spacer(Modifier.height(4.dp))
                            Text("Code: ${link.codeSystem}", fontSize = 13.sp, color = Color.Gray)
                            Text("Email: ${link.email}", fontSize = 13.sp, color = Color.Gray)
                            Spacer(Modifier.height(12.dp))
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            val resultMessage = executeLink(link)
                                            snackbarHostState.showSnackbar(resultMessage)
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E6F5C))
                                ) {
                                    Text("اجرا")
                                }
                                IconButton(onClick = { editingLink = link; showDialog = true }) {
                                    Icon(Icons.Default.Edit, null, tint = Color(0xFF1E6F5C))
                                }
                                IconButton(onClick = {
                                    saveLinks(context, links.filterNot { it.id == link.id })
                                    links = loadLinks(context)
                                }) {
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
                val current = links.toMutableList()
                if (editingLink == null) current.add(newLink)
                else {
                    val idx = current.indexOfFirst { it.id == newLink.id }
                    if (idx != -1) current[idx] = newLink
                }
                saveLinks(context, current)
                links = loadLinks(context)
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