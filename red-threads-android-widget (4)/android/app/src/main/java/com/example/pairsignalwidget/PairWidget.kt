package com.example.pairsignalwidget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.defaultWeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class PairWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.getSharedPreferences("pair_widget_prefs", Context.MODE_PRIVATE)
        val pairCode = prefs.getString("pair_code", "SYNC-777") ?: "SYNC-777"
        val role = prefs.getString("user_role", "user1") ?: "user1"

        // Fetch partner's status color from backend API
        val partnerColorHex = withContext(Dispatchers.IO) {
            fetchPartnerColor(pairCode, role)
        }

        provideContent {
            WidgetContent(
                pairCode = pairCode,
                role = role,
                partnerColorHex = partnerColorHex
            )
        }
    }

    @Composable
    private fun WidgetContent(
        pairCode: String,
        role: String,
        partnerColorHex: String
    ) {
        val beaconColor = parseColor(partnerColorHex)

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFF0F172A)) // Dark slate background
                .cornerRadius(24.dp)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Red Threads",
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = pairCode,
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFFDA4AF)),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = GlanceModifier.clickable(actionRunCallback<RefreshAction>())
                )
            }

            Spacer(modifier = GlanceModifier.height(10.dp))

            // Partner Status Glow Orb
            Box(
                modifier = GlanceModifier
                    .size(56.dp)
                    .background(beaconColor)
                    .cornerRadius(28.dp),
                contentAlignment = Alignment.Center
            ) {
                // Inner center point
                Box(
                    modifier = GlanceModifier
                        .size(18.dp)
                        .background(Color.White.copy(alpha = 0.85f))
                        .cornerRadius(9.dp)
                ) {}
            }

            Spacer(modifier = GlanceModifier.height(12.dp))

            // 3 Action Buttons: Heart (❤️), Eyes (👀), Hush (🤫)
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ColorButton(
                    label = "❤️ Heart",
                    color = Color(0xFF34D399),
                    actionColor = "green",
                    pairCode = pairCode,
                    role = role
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
                ColorButton(
                    label = "👀 Eyes",
                    color = Color(0xFFFBBF24),
                    actionColor = "yellow",
                    pairCode = pairCode,
                    role = role
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
                ColorButton(
                    label = "🤫 Hush",
                    color = Color(0xFFFB7185),
                    actionColor = "red",
                    pairCode = pairCode,
                    role = role
                )
            }
        }
    }

    @Composable
    private fun ColorButton(
        label: String,
        color: Color,
        actionColor: String,
        pairCode: String,
        role: String
    ) {
        val params = actionParametersOf(
            ColorParamKey to actionColor,
            CodeParamKey to pairCode,
            RoleParamKey to role
        )

        Button(
            text = label,
            onClick = actionRunCallback<SetStatusAction>(params),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = ColorProvider(color),
                contentColor = ColorProvider(Color(0xFF020617))
            ),
            style = TextStyle(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = GlanceModifier
                .height(36.dp)
                .cornerRadius(12.dp)
        )
    }

    private fun parseColor(hex: String): Color {
        return try {
            val clean = hex.trim().removePrefix("#")
            when (clean.uppercase()) {
                "34D399", "10B981" -> Color(0xFF10B981)
                "FBBF24", "F59E0B" -> Color(0xFFF59E0B)
                "F43F5E", "EF4444" -> Color(0xFFF43F5E)
                else -> Color(0xFF475569) // Grey offline
            }
        } catch (e: Exception) {
            Color(0xFF475569)
        }
    }

    companion object {
        val ColorParamKey = ActionParameters.Key<String>("status_color")
        val CodeParamKey = ActionParameters.Key<String>("pair_code")
        val RoleParamKey = ActionParameters.Key<String>("user_role")

        private const val BASE_URL = "https://ais-pre-7auili2vqqwu5ptslrqwt7-14204692651.us-east1.run.app"
        private val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()

        fun fetchPartnerColor(code: String, role: String): String {
            return try {
                val url = "$BASE_URL/api/pair/$code/color?role=$role"
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        response.body?.string()?.trim() ?: "#475569"
                    } else {
                        "#475569"
                    }
                }
            } catch (e: Exception) {
                "#475569"
            }
        }

        fun sendStatusUpdate(code: String, role: String, color: String): Boolean {
            return try {
                val url = "$BASE_URL/api/pair/$code/set/$color?role=$role&format=json"
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    response.isSuccessful
                }
            } catch (e: Exception) {
                false
            }
        }
    }
}

// Action Callback when tapping a color button
class SetStatusAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val color = parameters[PairWidget.ColorParamKey] ?: return
        val code = parameters[PairWidget.CodeParamKey] ?: "SYNC-777"
        val role = parameters[PairWidget.RoleParamKey] ?: "user1"

        withContext(Dispatchers.IO) {
            PairWidget.sendStatusUpdate(code, role, color)
        }

        PairWidget().update(context, glanceId)
    }
}

// Action Callback when tapping refresh
class RefreshAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        PairWidget().update(context, glanceId)
    }
}
