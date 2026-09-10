package com.example.pairsignalwidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("pair_widget_prefs", Context.MODE_PRIVATE)
        val initialCode = prefs.getString("pair_code", "SYNC-777") ?: "SYNC-777"
        val initialRole = prefs.getString("user_role", "user1") ?: "user1"

        setContent {
            var code by remember { mutableStateOf(initialCode) }
            var role by remember { mutableStateOf(initialRole) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF020617)) // Slate 950
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Red Threads",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "Configure your native home screen widget (❤️, 👀, 🤫)",
                    fontSize = 13.sp,
                    color = Color(0xFFFDA4AF),
                    modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
                )

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase().trim() },
                    label = { Text("Pair Code (e.g. SYNC-777)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Select your device role:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFE2E8F0),
                    modifier = Modifier.align(Alignment.Start)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = role == "user1",
                        onClick = { role = "user1" }
                    )
                    Text("Person 1 (Created the link)", color = Color.White, modifier = Modifier.padding(start = 4.dp))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = role == "user2",
                        onClick = { role = "user2" }
                    )
                    Text("Person 2 (Joined partner)", color = Color.White, modifier = Modifier.padding(start = 4.dp))
                }

                Button(
                    onClick = {
                        prefs.edit()
                            .putString("pair_code", code)
                            .putString("user_role", role)
                            .apply()

                        CoroutineScope(Dispatchers.IO).launch {
                            PairWidget().updateAll(this@MainActivity)
                        }

                        Toast.makeText(this@MainActivity, "Saved! Add the widget to your home screen.", Toast.LENGTH_LONG).show()

                        // If Android 8.0+, prompt user to place widget directly on home screen
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val appWidgetManager = getSystemService(AppWidgetManager::class.java)
                            val myProvider = ComponentName(this@MainActivity, PairWidgetReceiver::class.java)
                            if (appWidgetManager?.isRequestPinAppWidgetSupported == true) {
                                appWidgetManager.requestPinAppWidget(myProvider, null, null)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Save & Pin Widget to Home Screen", fontWeight = FontWeight.Bold, color = Color(0xFF020617))
                }
            }
        }
    }
}
