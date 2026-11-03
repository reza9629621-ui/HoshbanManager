package com.example.hoshban

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

// ------------------ مدل داده‌ها ------------------
data class LinkConfig(
    val id: String,
    val name: String,
    val codeSystem: String,
    val email: String
)

// ------------------ توابع ذخیره و خواندن ------------------
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

// ------------------ Activity ------------------
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
                AppNavigator()
            }
        }
    }
}

// ------------------ مدیریت صفحه‌ها ------------------
@Composable
fun AppNavigator() {
    // اگر این متغیر مقدار داشته باشد، یعنی باید WebView باز شود
    var activeUrl by remember { mutableStateOf<String?>(null) }

    if (activeUrl != null) {
        // صفحه وب (WebView)
        WebViewScreen(
            url = activeUrl!!,
            onBack = { activeUrl = null }
        )
    } else {
        // صفحه اصلی (لیست لینک‌ها)
        MainScreen(
            onExecute = { url -> activeUrl = url }
        )
    }
}

// ------------------ صفحه اصلی ------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onExecute: (String) -> Unit) {
    val context = LocalContext.current
    var links by remember { mutableStateOf(loadLinks(context)) }
    var showDialog by remember { mutableStateOf(false) }
    var editingLink by remember { mutableStateOf<LinkConfig?>(null) }

    Scaffold(
        topBar = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0xFF0F4C3A), Color(0xFF4CAF8F))))
                    .statusBarsPadding()
                    .padding(24.dp)
            ) {
                Column {
                    Text("Hoshban Manager", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    Text("مدیریت لینک‌ها", color = Color.White.copy(0.8f), fontSize = 16.sp)
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
                                        // ساخت لینک و ارسال به WebView
                                        val url = "https://hoshban.ir/gps/user/save_session.php?Code_system=${link.codeSystem}&email=${link.email}"
                                        onExecute(url)
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E6F5C))
                                ) {
                                    Text("ورود به سایت")
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

// ------------------ صفحه WebView (مرورگر داخلی) ------------------
@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen(url: String, onBack: () -> Unit) {
    // مدیریت دکمه بازگشت گوشی
    BackHandler {
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("سایت هوشبان", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E6F5C),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        AndroidView(
            modifier = Modifier.padding(padding).fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    webViewClient = WebViewClient()
                    settings.javaScriptEnabled = true // فعال کردن جاوااسکریپت برای سایت‌ها
                    settings.domStorageEnabled = true
                    loadUrl(url)
                }
            }
        )
    }
}

// ------------------ دیالوگ افزودن/ویرایش ------------------
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