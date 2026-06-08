package com.automatelinux.veggieBox.ui.greeting

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.automatelinux.veggieBox.ui.MainViewModel
import com.automatelinux.veggieBox.ui.route.Green
import com.automatelinux.veggieBox.util.Intents

@Composable
fun GreetingScreen(vm: MainViewModel) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current
    val targets = state.greet

    Column(Modifier.fillMaxSize()) {
        Surface(color = Green.copy(alpha = 0.08f)) {
            Text(
                "${targets.size} לקוחות ללא כתובת. כל לחיצה פותחת וואטסאפ עם הודעה מוכנה שמבקשת מהם למלא כתובת + הוראות הגעה (נשלח מהמספר שלך, כדי שיכירו אותך).",
                Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (targets.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("🎉 לכל הלקוחות יש כתובת!", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(targets, key = { it.customerId }) { t ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(t.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                            if (t.waUrl != null) {
                                Button(
                                    onClick = { Intents.whatsapp(ctx, t.waUrl) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Green),
                                ) {
                                    Icon(Icons.Filled.Send, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("בקש פרטים")
                                }
                            } else {
                                Text("אין טלפון", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}
