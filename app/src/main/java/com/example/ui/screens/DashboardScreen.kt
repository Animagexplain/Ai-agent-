package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.MoodLogEntity
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.ReminderEntity
import com.example.ui.theme.CyanDark
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.MoodChill
import com.example.ui.theme.MoodHappy
import com.example.ui.theme.MoodMotivated
import com.example.ui.theme.MoodSad
import com.example.ui.theme.MoodStressed
import com.example.ui.theme.MoodTired
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.viewmodel.JarvisViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val reminders by viewModel.reminders.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val moodLogs by viewModel.moodLogs.collectAsState()

    var showAddReminderDialog by remember { mutableStateOf(false) }
    var showAddNoteDialog by remember { mutableStateOf(false) }

    var selectedMood by remember { mutableStateOf("Chill") }
    var moodNoteInput by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Top Overview Banner
        Surface(
            color = DarkSurface,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyanDark.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(CyanDark.copy(alpha = 0.3f))
                        .border(1.dp, CyanPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Rika Memory & Productivity Hub 💜",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Queen of Curses • Gujranwala & Anime Hub",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Section 1: Reminders Card
        RemindersCard(
            reminders = reminders,
            onToggle = { viewModel.toggleReminder(it) },
            onDelete = { viewModel.deleteReminder(it.id) },
            onAddClicked = { showAddReminderDialog = true }
        )

        // Section 2: Quick Notes Card
        QuickNotesCard(
            notes = notes,
            onDelete = { viewModel.deleteNote(it.id) },
            onAddClicked = { showAddNoteDialog = true }
        )

        // Section 3: Mood Tracking Card
        MoodTrackerCard(
            moodLogs = moodLogs,
            selectedMood = selectedMood,
            onMoodSelected = { selectedMood = it },
            moodNote = moodNoteInput,
            onNoteChanged = { moodNoteInput = it },
            onLogMood = {
                viewModel.logMood(selectedMood, moodNoteInput)
                moodNoteInput = ""
            }
        )

        Spacer(modifier = Modifier.height(32.dp))
    }

    // Dialogs
    if (showAddReminderDialog) {
        AddReminderDialog(
            onDismiss = { showAddReminderDialog = false },
            onConfirm = { text, dt ->
                viewModel.addManualReminder(text, dt)
                showAddReminderDialog = false
            }
        )
    }

    if (showAddNoteDialog) {
        AddNoteDialog(
            onDismiss = { showAddNoteDialog = false },
            onConfirm = { noteText ->
                viewModel.addManualNote(noteText)
                showAddNoteDialog = false
            }
        )
    }
}

@Composable
fun RemindersCard(
    reminders: List<ReminderEntity>,
    onToggle: (ReminderEntity) -> Unit,
    onDelete: (ReminderEntity) -> Unit,
    onAddClicked: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Today's Reminders & Tasks",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    val pendingCount = reminders.count { !it.isCompleted }
                    if (pendingCount > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = CyanDark.copy(alpha = 0.3f),
                            shape = CircleShape
                        ) {
                            Text(
                                text = "$pendingCount",
                                color = CyanGlow,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onAddClicked,
                    modifier = Modifier.size(32.dp).testTag("add_reminder_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Reminder",
                        tint = CyanPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (reminders.isEmpty()) {
                Text(
                    text = "Koi pending reminder nahi hai. Jarivs se bolo: 'Aaj 5 baje college assignment ka reminder set karo'.",
                    color = TextTertiary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    reminders.forEach { reminder ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(DarkSurfaceVariant)
                                .clickable { onToggle(reminder) }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = reminder.isCompleted,
                                onCheckedChange = { onToggle(reminder) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = CyanPrimary,
                                    uncheckedColor = TextSecondary,
                                    checkmarkColor = Color.Black
                                )
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = reminder.text,
                                    color = if (reminder.isCompleted) TextTertiary else TextPrimary,
                                    fontSize = 14.sp,
                                    textDecoration = if (reminder.isCompleted) TextDecoration.LineThrough else null
                                )
                                Text(
                                    text = reminder.datetimeString,
                                    color = CyanGlow,
                                    fontSize = 11.sp
                                )
                            }
                            IconButton(
                                onClick = { onDelete(reminder) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = TextTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MoodTrackerCard(
    moodLogs: List<MoodLogEntity>,
    selectedMood: String,
    onMoodSelected: (String) -> Unit,
    moodNote: String,
    onNoteChanged: (String) -> Unit,
    onLogMood: () -> Unit
) {
    val moods = listOf(
        "Happy" to ("😊" to MoodHappy),
        "Chill" to ("🧊" to MoodChill),
        "Motivated" to ("⚡" to MoodMotivated),
        "Stressed" to ("🤯" to MoodStressed),
        "Tired" to ("😴" to MoodTired),
        "Sad" to ("🥺" to MoodSad)
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Mood,
                    contentDescription = null,
                    tint = MoodMotivated,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Emotional Check-in & Mood Tracker",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Mood Selector Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                moods.forEach { (name, pair) ->
                    val (emoji, color) = pair
                    val isSelected = selectedMood == name
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) color.copy(alpha = 0.2f) else DarkSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) color else DarkCardBorder
                        ),
                        modifier = Modifier
                            .clickable { onMoodSelected(name) }
                            .padding(2.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(emoji, fontSize = 18.sp)
                            Text(
                                text = name,
                                fontSize = 10.sp,
                                color = if (isSelected) color else TextSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = moodNote,
                    onValueChange = onNoteChanged,
                    placeholder = { Text("Kyun aisa feel ho raha hai? (Optional note)", fontSize = 12.sp, color = TextTertiary) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = CyanPrimary,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedContainerColor = DarkSurfaceVariant,
                        unfocusedContainerColor = DarkSurfaceVariant
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onLogMood,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("log_mood_button")
                ) {
                    Text("Log", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Recent mood history chips
            if (moodLogs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Recent Mood History:", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    moodLogs.take(4).forEach { log ->
                        val timeFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                        val timeStr = timeFormat.format(Date(log.timestamp))
                        Surface(
                            color = DarkSurfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = log.mood,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = CyanGlow
                                    )
                                    if (log.note.isNotBlank()) {
                                        Text(
                                            text = " — \"${log.note}\"",
                                            fontSize = 12.sp,
                                            color = TextPrimary
                                        )
                                    }
                                }
                                Text(
                                    text = timeStr,
                                    fontSize = 10.sp,
                                    color = TextTertiary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickNotesCard(
    notes: List<NoteEntity>,
    onDelete: (NoteEntity) -> Unit,
    onAddClicked: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Note,
                        contentDescription = null,
                        tint = Color(0xFF34D399),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Quick Notes",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onAddClicked,
                    modifier = Modifier.size(32.dp).testTag("add_note_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Note",
                        tint = Color(0xFF34D399)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (notes.isEmpty()) {
                Text(
                    text = "Koi note saved nahi hai. Rika ko boliye: 'Note save karo: Technical lab presentation Friday ko hai'.",
                    color = TextTertiary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    notes.take(5).forEach { note ->
                        Surface(
                            color = DarkSurfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = note.content,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { onDelete(note) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = TextTertiary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Dialogs
@Composable
fun AddReminderDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var dt by remember { mutableStateOf("Today 6:00 PM") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = { Text("Add New Reminder", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Task Description") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = CyanPrimary
                    )
                )
                OutlinedTextField(
                    value = dt,
                    onValueChange = { dt = it },
                    label = { Text("When (e.g. Shaam 6 baje / Tomorrow 9 AM)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = CyanPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (text.isNotBlank()) onConfirm(text, dt) },
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = Color.Black)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

@Composable
fun AddNoteDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = { Text("Save Quick Note", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Note content") },
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = Color(0xFF34D399)
                )
            )
        },
        confirmButton = {
            Button(
                onClick = { if (text.isNotBlank()) onConfirm(text) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34D399), contentColor = Color.Black)
            ) {
                Text("Save Note")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}
