package com.campus.service.ui.student

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.campus.service.data.model.Area
import com.campus.service.data.model.Category
import com.campus.service.ui.common.DropdownField
import com.campus.service.ui.common.LoadingButton
import com.campus.service.ui.common.MessageText

private const val TITLE_MAX = 100
private const val DESCRIPTION_MAX = 500

@Composable
fun CreateTicketScreen(vm: CreateTicketViewModel) {
    LaunchedEffect(Unit) { vm.loadOptionsIfNeeded() }

    val context = LocalContext.current
    val state by vm.state.collectAsState()

    var area by remember(state.formVersion) { mutableStateOf<Area?>(null) }
    var category by remember(state.formVersion) { mutableStateOf<Category?>(null) }
    var title by remember(state.formVersion) { mutableStateOf("") }
    var description by remember(state.formVersion) { mutableStateOf("") }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(MAX_IMAGES)
    ) { uris ->
        vm.addImages(context, uris)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("Tạo phiếu yêu cầu", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Báo cáo sự cố cơ sở vật chất, CNTT, phòng học...",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(16.dp))

        if (state.loadingOptions) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
        }

        DropdownField(
            label = "Khu vực / phòng",
            items = state.areas,
            selected = area,
            itemLabel = { it.label },
            onSelected = { area = it },
            enabled = state.areas.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        DropdownField(
            label = "Loại lỗi",
            items = state.categories,
            selected = category,
            itemLabel = { it.name },
            onSelected = { category = it },
            enabled = state.categories.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = title,
            onValueChange = { title = it.take(TITLE_MAX) },
            label = { Text("Tiêu đề") },
            supportingText = { Text("${title.length}/$TITLE_MAX") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = description,
            onValueChange = { description = it.take(DESCRIPTION_MAX) },
            label = { Text("Mô tả chi tiết") },
            supportingText = { Text("${description.length}/$DESCRIPTION_MAX") },
            minLines = 4,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        // ---------- Ảnh đính kèm ----------
        Text(
            "Ảnh minh họa (${state.images.size}/$MAX_IMAGES)",
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(Modifier.height(8.dp))
        if (state.images.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.images.forEachIndexed { index, image ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            bitmap = image.thumbnail.asImageBitmap(),
                            contentDescription = "Ảnh ${index + 1}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(96.dp)
                        )
                        TextButton(onClick = { vm.removeImage(index) }) { Text("Xóa") }
                    }
                }
            }
        }
        if (state.processingImages) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.size(8.dp))
                Text("Đang xử lý ảnh...")
            }
        }
        OutlinedButton(
            onClick = {
                imagePicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            enabled = state.images.size < MAX_IMAGES && !state.processingImages,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (state.images.size < MAX_IMAGES) "Chọn ảnh" else "Đã đủ $MAX_IMAGES ảnh")
        }
        Spacer(Modifier.height(12.dp))

        MessageText(error = state.error, info = state.message)

        if (state.areas.isEmpty() && !state.loadingOptions) {
            TextButton(onClick = { vm.loadOptionsIfNeeded() }) { Text("Tải lại danh sách") }
        }

        LoadingButton("Gửi phiếu", state.submitting) {
            val selectedArea = area
            val selectedCategory = category
            when {
                selectedArea == null -> vm.showError("Vui lòng chọn khu vực / phòng.")
                selectedCategory == null -> vm.showError("Vui lòng chọn loại lỗi.")
                title.trim().length < 5 -> vm.showError("Tiêu đề tối thiểu 5 ký tự.")
                description.trim().length < 10 -> vm.showError("Mô tả tối thiểu 10 ký tự.")
                state.processingImages -> vm.showError("Đang xử lý ảnh, vui lòng đợi.")
                else -> vm.submit(selectedArea, selectedCategory, title, description)
            }
        }
    }
}