package com.campus.service.ui.common

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.campus.service.data.repository.TicketRepository
import com.campus.service.util.ImageCache

/** Hiển thị ảnh lưu trong Supabase Storage (bucket ticket-images) theo đường dẫn. */
@Composable
fun StorageImage(path: String, modifier: Modifier = Modifier) {
    var bitmap by remember(path) { mutableStateOf<Bitmap?>(ImageCache.get(path)) }
    var failed by remember(path) { mutableStateOf(false) }

    LaunchedEffect(path) {
        if (bitmap == null) {
            val loaded = try {
                TicketRepository.loadImage(path)
            } catch (e: Exception) {
                null
            }
            if (loaded != null) {
                ImageCache.put(path, loaded)
                bitmap = loaded
            } else {
                failed = true
            }
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val current = bitmap
        when {
            current != null -> Image(
                bitmap = current.asImageBitmap(),
                contentDescription = "Ảnh minh họa",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            failed -> Text("Không tải được ảnh")
            else -> CircularProgressIndicator()
        }
    }
}