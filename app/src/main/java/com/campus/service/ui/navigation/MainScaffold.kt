package com.campus.service.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.campus.service.data.model.Profile
import com.campus.service.data.model.UserRole
import com.campus.service.ui.profile.ProfileScreen
import com.campus.service.ui.profile.ProfileViewModel

private data class TabItem(
    val title: String,
    val icon: ImageVector,
    val isProfile: Boolean = false
)

private fun tabsFor(role: UserRole): List<TabItem> = when (role) {
    UserRole.STUDENT -> listOf(
        TabItem("Trang chủ", Icons.Filled.Home),
        TabItem("Tạo phiếu", Icons.Filled.Add),
        TabItem("Lịch sử", Icons.Filled.DateRange),
        TabItem("Cá nhân", Icons.Filled.Person, isProfile = true)
    )
    UserRole.ADMIN -> listOf(
        TabItem("Tổng quan", Icons.Filled.Home),
        TabItem("Điều phối", Icons.Filled.Build),
        TabItem("Lịch sử", Icons.Filled.DateRange),
        TabItem("Cá nhân", Icons.Filled.Person, isProfile = true)
    )
    UserRole.TECHNICIAN -> listOf(
        TabItem("Radar", Icons.Filled.Search),
        TabItem("Công việc", Icons.Filled.Build),
        TabItem("Lịch sử", Icons.Filled.DateRange),
        TabItem("Cá nhân", Icons.Filled.Person, isProfile = true)
    )
}

@Composable
fun MainScaffold(
    profile: Profile,
    profileViewModel: ProfileViewModel,
    onSignOut: () -> Unit
) {
    val role = profile.userRole
    val tabs = remember(role) { tabsFor(role) }
    var selected by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = { selected = index },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            val tab = tabs[selected.coerceIn(0, tabs.lastIndex)]
            if (tab.isProfile) {
                ProfileScreen(
                    profile = profile,
                    vm = profileViewModel,
                    onSignOut = onSignOut
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(tab.title, style = MaterialTheme.typography.headlineMedium)
                    Text("(${role.displayName}, màn hình sẽ làm ở các mục sau)")
                }
            }
        }
    }
}