package com.example.hoshban

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalLayoutDirection
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

private const val PREFS_NAME = "hoshban_manager"
private const val PREFS_KEY = "saved_accounts"

private val Navy = Color(0xFF123A70)
private val Blue = Color(0xFF246BCE)
private val Teal = Color(0xFF12A89D)
private val Background = Color(0xFFF3F6FC)
private val TextDark = Color(0xFF172B4D)

data class HoshbanAccount(
    val id: Long,
    val name: String,
    val codeSystem: String,
    val email: String,
    val language: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HoshbanManagerApp()
        }
    }
}

private fun loadAccounts(context: Context): List<HoshbanAccount> {
    val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(PREFS_KEY, "[]") ?: "[]"

    return try {
        val array = JSONArray(json)

        (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)

            HoshbanAccount(
                id = item.optLong("id"),
                name = item.optString("name"),
                codeSystem = item.optString("codeSystem"),
                email = item.optString("email"),
                language = item.optString("language", "فارسی")
            )
        }
    } catch (_: Exception) {
        emptyList()
    }
}

private fun saveAccounts(
    context: Context,
    accounts: List<HoshbanAccount>
) {
    val array = JSONArray()

    accounts.forEach { account ->
        val item = JSONObject().apply {
            put("id", account.id)
            put("name", account.name)
            put("codeSystem", account.codeSystem)
            put("email", account.email)
            put("language", account.language)
        }
        array.put(item)
    }

    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(PREFS_KEY, array.toString())
        .apply()
}

private fun createSessionUrl(account: HoshbanAccount): String {
    val code = URLEncoder.encode(account.codeSystem, "UTF-8")
    val email = URLEncoder.encode(account.email, "UTF-8")

    return "https://hoshban.ir/gps/user/save_session.php" +
            "?Code_system=$code&email1=$email"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HoshbanManagerApp() {
    val context = LocalContext.current

    val accounts = remember {
        mutableStateListOf<HoshbanAccount>().apply {
            addAll(loadAccounts(context))
        }
    }

    var selectedLanguage by remember { mutableStateOf("همه") }
    var showDialog by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<HoshbanAccount?>(null) }

    val languages = listOf("همه", "فارسی", "English", "العربية")

    val visibleAccounts = if (selectedLanguage == "همه") {
        accounts.toList()
    } else {
        accounts.filter { it.language == selectedLanguage }
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl
    ) {
        MaterialTheme(
            colorScheme = lightColorScheme(
                primary = Blue,
                secondary = Teal,
                background = Background,
                surface = Color.White
            )
        ) {
            Scaffold(
                containerColor = Background,
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        onClick = {
                            editingAccount = null
                            showDialog = true
                        },
                        containerColor = Blue,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(18.dp),
                        icon = {
                            Icon(Icons.Default.Add, contentDescription = null)
                        },
                        text = {
                            Text(
                                "افزودن حساب",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )
                }
            ) { padding ->

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(Modifier.height(12.dp))

                    // Header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(28.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Navy,
                                        Blue,
                                        Color(0xFF438DE5)
                                    )
                                )
                            )
                            .padding(22.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(17.dp))
                                        .background(
                                            Color.White.copy(alpha = 0.18f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Hub,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }

                                Spacer(Modifier.width(12.dp))

                                Column {
                                    Text(
                                        "هوشبان",
                                        color = Color.White,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.ExtraBold
                                    )

                                    Text(
                                        "مدیریت هوشمند حساب‌ها",
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }

                            Spacer(Modifier.height(22.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "تعداد حساب‌های ذخیره‌شده",
                                        color = Color.White.copy(alpha = 0.8f)
                                    )

                                    Text(
                                        accounts.size.toString(),
                                        color = Color.White,
                                        style = MaterialTheme.typography.headlineLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(Modifier.weight(1f))

                                Icon(
                                    Icons.Default.ShieldCheck,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(42.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Text(
                        "انتخاب زبان",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )

                    Spacer(Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        languages.forEach { language ->
                            FilterChip(
                                selected = selectedLanguage == language,
                                onClick = {
                                    selectedLanguage = language
                                },
                                label = {
                                    Text(language)
                                },
                                shape = RoundedCornerShape(14.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "حساب‌های من",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )

                        Spacer(Modifier.weight(1f))

                        Surface(
                            color = Color(0xFFE5EEFC),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "${visibleAccounts.size} مورد",
                                modifier = Modifier.padding(
                                    horizontal = 12.dp,
                                    vertical = 6.dp
                                ),
                                color = Navy
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    if (visibleAccounts.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(90.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE7EFFB)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.PersonAddAlt1,
                                        contentDescription = null,
                                        tint = Blue,
                                        modifier = Modifier.size(42.dp)
                                    )
                                }

                                Spacer(Modifier.height(14.dp))

                                Text(
                                    "هنوز حسابی اضافه نشده",
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )

                                Spacer(Modifier.height(6.dp))

                                Text(
                                    "برای شروع، حساب جدید اضافه کنید",
                                    color = Color(0xFF73819A)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = visibleAccounts,
                                key = { it.id }
                            ) { account ->

                                AccountCard(
                                    account = account,
                                    onOpen = {
                                        try {
                                            val intent = Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse(createSessionUrl(account))
                                            )
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                            Toast.makeText(
                                                context,
                                                "مرورگر در دسترس نیست",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    onEdit = {
                                        editingAccount = account
                                        showDialog = true
                                    },
                                    onDelete = {
                                        accounts.removeAll {
                                            it.id == account.id
                                        }
                                        saveAccounts(context, accounts)

                                        Toast.makeText(
                                            context,
                                            "حساب حذف شد",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (showDialog) {
                AccountDialog(
                    initialAccount = editingAccount,
                    onDismiss = {
                        showDialog = false
                        editingAccount = null
                    },
                    onSave = { name, code, email, language ->

                        if (
                            name.isBlank() ||
                            code.isBlank() ||
                            email.isBlank()
                        ) {
                            Toast.makeText(
                                context,
                                "لطفاً همه فیلدها را تکمیل کنید",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            val updated = HoshbanAccount(
                                id = editingAccount?.id
                                    ?: System.currentTimeMillis(),
                                name = name.trim(),
                                codeSystem = code.trim(),
                                email = email.trim(),
                                language = language
                            )

                            if (editingAccount == null) {
                                accounts.add(0, updated)
                            } else {
                                val index = accounts.indexOfFirst {
                                    it.id == editingAccount!!.id
                                }

                                if (index >= 0) {
                                    accounts[index] = updated
                                }
                            }

                            saveAccounts(context, accounts)

                            showDialog = false
                            editingAccount = null
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun AccountCard(
    account: HoshbanAccount,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFFE2EEFF),
                                    Color(0xFFD9F6F0)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = Blue,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(Modifier.width(10.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        account.name,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        account.language,
                        color = Teal,
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "ویرایش",
                        tint = Color(0xFF64748B)
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "حذف",
                        tint = Color(0xFFE05A67)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            HorizontalDivider(color = Color(0xFFF0F2F6))

            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Tag,
                    contentDescription = null,
                    tint = Color(0xFF8492A8),
                    modifier = Modifier.size(18.dp)
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    "کد سیستم: ${account.codeSystem}",
                    color = Color(0xFF53627A),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Email,
                    contentDescription = null,
                    tint = Color(0xFF8492A8),
                    modifier = Modifier.size(18.dp)
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    account.email,
                    color = Color(0xFF53627A),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(14.dp))

            Button(
                onClick = onOpen,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Blue
                )
            ) {
                Icon(
                    Icons.Default.OpenInBrowser,
                    contentDescription = null
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    "ورود به حساب",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AccountDialog(
    initialAccount: HoshbanAccount?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var name by remember(initialAccount) {
        mutableStateOf(initialAccount?.name ?: "")
    }

    var code by remember(initialAccount) {
        mutableStateOf(initialAccount?.codeSystem ?: "")
    }

    var email by remember(initialAccount) {
        mutableStateOf(initialAccount?.email ?: "")
    }

    var language by remember(initialAccount) {
        mutableStateOf(initialAccount?.language ?: "فارسی")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initialAccount == null)
                    "افزودن حساب جدید"
                else
                    "ویرایش حساب",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام حساب") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Code_system") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("ایمیل") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    "زبان حساب",
                    fontWeight = FontWeight.Medium
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    listOf("فارسی", "English", "العربية").forEach {
                        FilterChip(
                            selected = language == it,
                            onClick = { language = it },
                            label = { Text(it) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(name, code, email, language)
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("ذخیره")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
