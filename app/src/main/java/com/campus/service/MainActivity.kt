package com.campus.service

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable

@Serializable
data class Area(
    val id: Int,
    val name: String,
    val building: String? = null
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var text by remember { mutableStateOf("Đang tải...") }

                LaunchedEffect(Unit) {
                    text = try {
                        val list = SupabaseProvider.client
                            .from("areas")
                            .select()
                            .decodeList<Area>()
                        "Kết nối OK: ${list.size} khu vực\n" +
                                list.take(3).joinToString("\n") { it.name }
                    } catch (e: Exception) {
                        "Lỗi: ${e.message}"
                    }
                }

                Box(modifier = Modifier.fillMaxSize().padding(32.dp)) {
                    Text(text)
                }
            }
        }
    }
}