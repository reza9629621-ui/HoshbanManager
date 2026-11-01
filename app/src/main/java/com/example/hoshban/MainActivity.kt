package com.example.hoshban

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

import org.json.JSONArray
import org.json.JSONObject

import java.net.URLEncoder


// --------------------------------------------------
// اطلاعات هر لینک
// --------------------------------------------------

data class SavedLink(
    val id: Long,
    val title: String,
    val codeSystem: String,
    val email: String,
    val language: String
)


// --------------------------------------------------
// تنظیمات ذخیره سازی
// --------------------------------------------------

private const val PREFS_NAME = "hoshban_links"
private const val LINKS_KEY = "saved_links"


// --------------------------------------------------
// صفحه اصلی برنامه
// --------------------------------------------------

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            HoshbanApp()
        }
    }
}


// --------------------------------------------------
// خواندن اطلاعات ذخیره شده
// --------------------------------------------------

private fun loadLinks(context: Context): List<SavedLink> {

    val prefs = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    val json = prefs.getString(LINKS_KEY, "[]") ?: "[]"

    val result = mutableListOf<SavedLink>()

    try {

        val array = JSONArray(json)

        for (i in 0 until array.length()) {

            val item = array.getJSONObject(i)

            result.add(
                SavedLink(
                    id = item.optLong("id"),
                    title = item.optString("title"),
                    codeSystem = item.optString("codeSystem"),
                    email = item.optString("email"),
                    language = item.optString("language", "فارسی")
                )
            )
        }

    } catch (e: Exception) {

        return emptyList()
    }

    return result
}


// --------------------------------------------------
// ذخیره اطلاعات لینک ها
// --------------------------------------------------

private fun saveLinks(
    context: Context,
    links: List<SavedLink>
) {

    val array = JSONArray()

    links.forEach { link ->

        val item = JSONObject()

        item.put("id", link.id)
        item.put("title", link.title)
        item.put("codeSystem", link.codeSystem)
        item.put("email", link.email)
        item.put("language", link.language)

        array.put(item)
    }

    context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    ).edit()
        .putString(LINKS_KEY, array.toString())
        .apply()
}


// --------------------------------------------------
// ساخت آدرس اصلی سایت
// --------------------------------------------------

private fun createUrl(link: SavedLink): String {

    val code = URLEncoder.encode(
        link.codeSystem,
        "UTF-8"
    )

    val email = URLEncoder.encode(
        link.email,
        "UTF-8"
    )

    return "https://hoshban.ir/gps/user/save_session.php" +
            "?Code_system=$code&email1=$email"
}


// --------------------------------------------------
// رابط اصلی برنامه
// --------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HoshbanApp() {

    val context = LocalContext.current

    val links = remember {

        mutableStateListOf<SavedLink>().apply {

            addAll(loadLinks(context))
        }
    }

    var selectedLanguage by remember {
        mutableStateOf("همه")
    }

    var showDialog by remember {
        mutableStateOf(false)
    }

    var editingLink by remember {
        mutableStateOf<SavedLink?>(null)
    }

    val filteredLinks = if (selectedLanguage == "همه") {
        links.toList()
    } else {
        links.filter {
            it.language == selectedLanguage
        }
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl
    ) {

        MaterialTheme(

            colorScheme = lightColorScheme(

                primary = Color(0xFF1769AA),

                secondary = Color(0xFF00A896),

                background = Color(0xFFF4F6FA),

                surface = Color.White
            )

        ) {

            Scaffold(

                containerColor = MaterialTheme.colorScheme.background,

                floatingActionButton = {

                    FloatingActionButton(

                        onClick = {

                            editingLink = null

                            showDialog = true
                        },

                        containerColor = MaterialTheme.colorScheme.primary

                    ) {

                        Icon(
                            Icons.Default.Add,
                            contentDescription = "افزودن لینک",
                            tint = Color.White
                        )
                    }
                }

            ) { padding ->

                Column(

                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)

                ) {

                    // عنوان برنامه

                    Text(

                        text = "هوشبان",

                        style = MaterialTheme.typography.headlineMedium,

                        fontWeight = FontWeight.Bold,

                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(

                        text = "مدیریت لینک های ارتباطی",

                        style = MaterialTheme.typography.bodyMedium,

                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(
                        modifier = Modifier.height(20.dp)
                    )


                    // انتخاب زبان

                    Card(

                        modifier = Modifier.fillMaxWidth(),

                        shape = RoundedCornerShape(16.dp)

                    ) {

                        Column(
                            modifier = Modifier.padding(14.dp)
                        ) {

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Icon(
                                    Icons.Default.Language,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )

                                Spacer(
                                    modifier = Modifier.width(8.dp)
                                )

                                Text(
                                    "انتخاب زبان",
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(
                                modifier = Modifier.height(12.dp)
                            )

                            Row(

                                modifier = Modifier.fillMaxWidth(),

                                horizontalArrangement = Arrangement.spacedBy(4.dp)

                            ) {

                                listOf(
                                    "همه",
                                    "فارسی",
                                    "English",
                                    "العربية"
                                ).forEach { language ->

                                    FilterChip(

                                        selected = selectedLanguage == language,

                                        onClick = {
                                            selectedLanguage = language
                                        },

                                        label = {
                                            Text(language)
                                        }
                                    )
                                }
                            }
                        }
                    }


                    Spacer(
                        modifier = Modifier.height(18.dp)
                    )


                    // عنوان لیست

                    Row(

                        modifier = Modifier.fillMaxWidth(),

                        horizontalArrangement = Arrangement.SpaceBetween

                    ) {

                        Text(
                            "لینک های ثبت شده",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            "${filteredLinks.size} لینک",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )


                    // نمایش لینک ها

                    if (filteredLinks.isEmpty()) {

                        Box(

                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),

                            contentAlignment = Alignment.Center

                        ) {

                            Column(

                                horizontalAlignment = Alignment.CenterHorizontally

                            ) {

                                Icon(
                                    Icons.Default.Link,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )

                                Spacer(
                                    modifier = Modifier.height(12.dp)
                                )

                                Text("هنوز لینکی ثبت نشده است")

                                Text(
                                    "برای افزودن لینک، دکمه + را بزنید",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                    } else {

                        LazyColumn(

                            modifier = Modifier.weight(1f),

                            verticalArrangement = Arrangement.spacedBy(10.dp)

                        ) {

                            items(
                                items = filteredLinks,
                                key = { it.id }
                            ) { link ->

                                LinkCard(

                                    link = link,

                                    onOpen = {

                                        try {

                                            val intent = Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse(createUrl(link))
                                            )

                                            context.startActivity(intent)

                                        } catch (e: Exception) {

                                            Toast.makeText(
                                                context,
                                                "امکان باز کردن لینک وجود ندارد",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },

                                    onEdit = {

                                        editingLink = link

                                        showDialog = true
                                    },

                                    onDelete = {

                                        links.remove(link)

                                        saveLinks(
                                            context,
                                            links.toList()
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }


            // پنجره افزودن یا ویرایش

            if (showDialog) {

                AddEditLinkDialog(

                    existingLink = editingLink,

                    onDismiss = {

                        showDialog = false

                        editingLink = null
                    },

                    onSave = { title, code, email, language ->

                        val oldLink = editingLink

                        if (oldLink == null) {

                            val newLink = SavedLink(

                                id = System.currentTimeMillis(),

                                title = title,

                                codeSystem = code,

                                email = email,

                                language = language
                            )

                            links.add(newLink)

                        } else {

                            val index = links.indexOfFirst {
                                it.id == oldLink.id
                            }

                            if (index >= 0) {

                                links[index] = oldLink.copy(

                                    title = title,

                                    codeSystem = code,

                                    email = email,

                                    language = language
                                )
                            }
                        }

                        saveLinks(
                            context,
                            links.toList()
                        )

                        showDialog = false

                        editingLink = null
                    }
                )
            }
        }
    }
}


// --------------------------------------------------
// کارت هر لینک
// --------------------------------------------------

@Composable
fun LinkCard(

    link: SavedLink,

    onOpen: () -> Unit,

    onEdit: () -> Unit,

    onDelete: () -> Unit

) {

    Card(

        modifier = Modifier.fillMaxWidth(),

        shape = RoundedCornerShape(16.dp),

        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),

        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )

    ) {

        Column(
            modifier = Modifier.padding(14.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    Icons.Default.Link,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(
                    modifier = Modifier.width(8.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        link.title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        "زبان: ${link.language}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onEdit
                ) {

                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "ویرایش"
                    )
                }

                IconButton(
                    onClick = onDelete
                ) {

                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "حذف",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                "کد سیستم: ${link.codeSystem}",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                "ایمیل: ${link.email}",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Button(

                onClick = onOpen,

                modifier = Modifier.fillMaxWidth()

            ) {

                Icon(
                    Icons.Default.OpenInBrowser,
                    contentDescription = null
                )

                Spacer(
                    modifier = Modifier.width(8.dp)
                )

                Text("باز کردن لینک")
            }
        }
    }
}


// --------------------------------------------------
// پنجره افزودن و ویرایش لینک
// --------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditLinkDialog(

    existingLink: SavedLink?,

    onDismiss: () -> Unit,

    onSave: (
        String,
        String,
        String,
        String
    ) -> Unit

) {

    var title by remember(existingLink) {
        mutableStateOf(existingLink?.title ?: "")
    }

    var code by remember(existingLink) {
        mutableStateOf(existingLink?.codeSystem ?: "")
    }

    var email by remember(existingLink) {
        mutableStateOf(existingLink?.email ?: "")
    }

    var language by remember(existingLink) {
        mutableStateOf(existingLink?.language ?: "فارسی")
    }

    var expanded by remember {
        mutableStateOf(false)
    }

    val languages = listOf(
        "فارسی",
        "English",
        "العربية"
    )

    AlertDialog(

        onDismissRequest = onDismiss,

        title = {

            Text(
                if (existingLink == null) {
                    "افزودن لینک جدید"
                } else {
                    "ویرایش لینک"
                }
            )
        },

        text = {

            Column(

                verticalArrangement = Arrangement.spacedBy(10.dp)

            ) {

                OutlinedTextField(

                    value = title,

                    onValueChange = {
                        title = it
                    },

                    label = {
                        Text("نام لینک")
                    },

                    modifier = Modifier.fillMaxWidth(),

                    singleLine = true
                )

                OutlinedTextField(

                    value = code,

                    onValueChange = {
                        code = it
                    },

                    label = {
                        Text("Code_system")
                    },

                    modifier = Modifier.fillMaxWidth(),

                    singleLine = true
                )

                OutlinedTextField(

                    value = email,

                    onValueChange = {
                        email = it
                    },

                    label = {
                        Text("Email")
                    },

                    modifier = Modifier.fillMaxWidth(),

                    singleLine = true
                )

                ExposedDropdownMenuBox(

                    expanded = expanded,

                    onExpandedChange = {
                        expanded = !expanded
                    }

                ) {

                    OutlinedTextField(

                        value = language,

                        onValueChange = {},

                        readOnly = true,

                        label = {
                            Text("زبان لینک")
                        },

                        trailingIcon = {

                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = expanded
                            )
                        },

                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(

                        expanded = expanded,

                        onDismissRequest = {
                            expanded = false
                        }

                    ) {

                        languages.forEach { item ->

                            DropdownMenuItem(

                                text = {
                                    Text(item)
                                },

                                onClick = {

                                    language = item

                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },

        confirmButton = {

            Button(

                onClick = {

                    if (
                        title.isNotBlank() &&
                        code.isNotBlank() &&
                        email.isNotBlank()
                    ) {

                        onSave(
                            title.trim(),
                            code.trim(),
                            email.trim(),
                            language
                        )
                    }
                },

                enabled = title.isNotBlank() &&
                        code.isNotBlank() &&
                        email.isNotBlank()

            ) {

                Text("ذخیره")
            }
        },

        dismissButton = {

            TextButton(
                onClick = onDismiss
            ) {

                Text("انصراف")
            }
        }
    )
}
