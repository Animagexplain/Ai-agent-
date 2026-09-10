package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyanDark
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.DarkSurfaceVariant

@Composable
fun QuickActionChips(
    onChipClicked: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val chips = listOf(
        "🎬 Anime Ideas" to "Jarvis, mere anime channel ke liye viral video ideas batao.",
        "⏰ Aaj ke Reminders" to "Mere aaj ke kya reminders hain?",
        "😊 Mood Log" to "Mera mood log karo: Bahut energetic aur motivated feel kar raha hoon!",
        "📝 Quick Note" to "Ek quick note save karo: Next video edit mein pacing fast rakhni hai.",
        "🎓 Study Check" to "College exams aur technical projects ka timetable organize karo.",
        "💬 Kaisa hai bhai?" to "Arey bhai, kaisa chal raha hai sab?"
    )

    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        chips.forEach { (label, prompt) ->
            AssistChip(
                onClick = { onChipClicked(prompt) },
                label = { Text(label, color = Color.White) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = DarkSurfaceVariant,
                    labelColor = Color.White
                ),
                border = AssistChipDefaults.assistChipBorder(
                    enabled = true,
                    borderColor = CyanDark.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}
