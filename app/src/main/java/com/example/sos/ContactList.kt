package com.example.sos

import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sos.contactCred.CryptoManager
import com.example.sos.contactCred.TrustedContact
import com.example.sos.contactCred.TrustedContactsViewModel

// -------------------- SCREEN --------------------

@Composable
fun TrustedContactsScreen(onBack: () -> Unit) {

    BackHandler { onBack() }
    val context = LocalContext.current

    var showRelationDialog by remember { mutableStateOf(false) }
    var selectedName by remember { mutableStateOf("") }
    var selectedPhone by remember { mutableStateOf("") }
    var relationText by remember { mutableStateOf("") }

    val viewModel : TrustedContactsViewModel = viewModel()
    val contacts by viewModel.contacts.collectAsState()

    // -------------------- ADD DIALOG --------------------

    if (showRelationDialog) {
        AlertDialog(
            onDismissRequest = { showRelationDialog = false },
            title = { Text("Add Relation") },
            text = {
                Column {
                    Text("$selectedName\n$selectedPhone", fontSize = 12.sp, color = Color.Gray)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = relationText,
                        onValueChange = { relationText = it },
                        label = { Text("Relation") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addContact(
                        name = selectedName,
                        phonePlain = selectedPhone,
                        relation = relationText.ifBlank { "Contact" }
                    )

                    relationText = ""
                    showRelationDialog = false
                }) { Text("ADD") }
            },
            dismissButton = {
                TextButton(onClick = { showRelationDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }

    // -------------------- CONTACT PICKER --------------------

    val contactPickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
            uri ?: return@rememberLauncherForActivityResult

            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return@use

                val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                if (nameIndex == -1 || idIndex == -1) return@use

                selectedName = cursor.getString(nameIndex)
                val contactId = cursor.getString(idIndex)

                context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID}=?",
                    arrayOf(contactId),
                    null
                )?.use { phoneCursor ->
                    if (phoneCursor.moveToFirst()) {
                        val phoneIndex =
                            phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        if (phoneIndex != -1) {
                            selectedPhone = phoneCursor.getString(phoneIndex)
                        }
                    }
                }

                showRelationDialog = true
            }
        }

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) contactPickerLauncher.launch(null)
        }

    // -------------------- UI --------------------

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1220))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {

            TrustedContactsTopBar(onBack)
            Spacer(Modifier.height(12.dp))
            DescriptionText()
            Spacer(Modifier.height(24.dp))

            Text(
                "MY CIRCLE (${contacts.size})",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(Modifier.height(12.dp))

            contacts.forEach { contact ->
                ContactItem(
                    contact = contact,
                    onDelete = { viewModel.deleteContact(contact) }
                )
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(80.dp))
        }

        FloatingActionButton(
            onClick = {
                if (ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.READ_CONTACTS
                    ) == PackageManager.PERMISSION_GRANTED
                ) contactPickerLauncher.launch(null)
                else permissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
            },
            containerColor = Color(0xFF2563EB),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
        }
    }
}

// -------------------- TOP BAR --------------------

@Composable
fun TrustedContactsTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = Color.White,
            modifier = Modifier.clickable { onBack() }
        )
        Spacer(Modifier.weight(1f))
        Text(
            "Trusted Contacts",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.weight(1f))
    }
}

// -------------------- DESCRIPTION --------------------

@Composable
fun DescriptionText() {
    Text(
        text = "Manage who receives your alerts. Active contacts will get an instant SOS notification with your live location and audio recording.",
        color = Color(0xFF94A3B8),
        fontSize = 14.sp,
        modifier = Modifier.padding(horizontal = 20.dp)
    )
}

// -------------------- CONTACT ITEM --------------------

@Composable
fun ContactItem(
    contact: TrustedContact,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .background(Color(0xFF0F172A), RoundedCornerShape(14.dp))
            .fillMaxWidth()
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(42.dp)
                .background(Color(0xFF1E293B), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                contact.name.first().uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(contact.name, color = Color.White, fontWeight = FontWeight.Medium)
                Spacer(Modifier.width(8.dp))
                RelationChip(contact.relation, Color(0xFF38BDF8))
            }

            Text(
                text = try { CryptoManager.decrypt(contact.phone) } catch (e: Exception) { "••••••••" },
                color = Color(0xFF94A3B8),
                fontSize = 12.sp
            )
        }

        Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = "Menu",
            tint = Color(0xFF94A3B8),
            modifier = Modifier.clickable { showMenu = true }
        )

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = {
                    showMenu = false
                    onDelete()
                }
            )
        }
    }
}

// -------------------- RELATION CHIP --------------------

@Composable
fun RelationChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(text, color = color, fontSize = 10.sp)
    }
}
