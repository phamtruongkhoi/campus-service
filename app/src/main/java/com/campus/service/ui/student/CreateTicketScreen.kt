package com.campus.service.ui.student

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.campus.service.data.model.Area
import com.campus.service.data.model.Category
import com.campus.service.ui.common.AppCard
import com.campus.service.ui.common.AppTextField
import com.campus.service.ui.common.DropdownField
import com.campus.service.ui.common.GradientHeader
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

    Column(modifier = Modifier.fillMaxSize()) {
        GradientHeader {
            Column {
                Text(
                    "Tạo phiếu yêu cầu",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
                Text(
                    "Báo cáo sự cố cơ sở vật chất, CNTT, phòng học...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (state.loadingOptions) {
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
            }

            AppCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Thông tin sự cố", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
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
                    AppTextField(
                        value = title,
                        onValueChange = { title = it.take(TITLE_MAX) },
                        label = "Tiêu đề",
                        supportingText = "${title.length}/$TITLE_MAX"
                    )
                    Spacer(Modifier.height(4.dp))
                    AppTextField(
                        value = description,
                        onValueChange = { description = it.take(DESCRIPTION_MAX) },
                        label = "Mô tả chi tiết",
                        supportingText = "${description.length}/$DESCRIPTION_MAX",
                        singleLine = false,
                        minLines = 4
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            AppCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Ảnh minh họa (${state.images.size}/$MAX_IMAGES)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    if (state.images.isNotEmpty()) {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            state.images.forEachIndexed { index, image ->
                                Box {
                                    Image(
                                        bitmap = image.thumbnail.asImageBitmap(),
                                        contentDescription = "Ảnh ${index + 1}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(96.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                    IconButton(
                                        onClick = { vm.removeImage(index) },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.55f))
                                    ) {
                                        Icon(
                                            Icons.Outlined.Close,
                                            contentDescription = "Xóa ảnh ${index + 1}",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    if (state.processingImages) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.size(8.dp))
                            Text("Đang xử lý ảnh...")
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    OutlinedButton(
                        onClick = {
                            imagePicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = state.images.size < MAX_IMAGES && !state.processingImages,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(Icons.Outlined.AddPhotoAlternate, null)
                        Spacer(Modifier.size(8.dp))
                        Text(if (state.images.size < MAX_IMAGES) "Chọn ảnh" else "Đã đủ $MAX_IMAGES ảnh")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
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
            Spacer(Modifier.height(24.dp))
        }
    }
}