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
import com.campus.service.ui.admin.AdminDashboardScreen
import com.campus.service.ui.admin.AdminDashboardViewModel
import com.campus.service.ui.admin.AdminDispatchScreen
import com.campus.service.ui.admin.AdminDispatchViewModel
import com.campus.service.ui.admin.AdminHistoryScreen
import com.campus.service.ui.admin.AdminHistoryViewModel
import com.campus.service.ui.profile.ProfileScreen
import com.campus.service.ui.profile.ProfileViewModel
import com.campus.service.ui.student.CreateTicketScreen
import com.campus.service.ui.student.CreateTicketViewModel
import com.campus.service.ui.student.HomeScreen
import com.campus.service.ui.student.HomeViewModel
import com.campus.service.ui.student.TicketHistoryScreen
import com.campus.service.ui.student.TicketHistoryViewModel
import com.campus.service.ui.technician.RadarScreen
import com.campus.service.ui.technician.RadarViewModel
import com.campus.service.ui.technician.TechHistoryScreen
import com.campus.service.ui.technician.TechHistoryViewModel
import com.campus.service.ui.technician.TechJobsScreen
import com.campus.service.ui.technician.TechJobsViewModel

private enum class TabKind {
    PLACEHOLDER, PROFILE, CREATE_TICKET, STUDENT_HISTORY, STUDENT_HOME,
    ADMIN_DASHBOARD, ADMIN_DISPATCH, ADMIN_HISTORY, TECH_JOBS, TECH_RADAR, TECH_HISTORY
}

private data class TabItem(
    val title: String,
    val icon: ImageVector,
    val kind: TabKind = TabKind.PLACEHOLDER
)

private fun tabsFor(role: UserRole): List<TabItem> = when (role) {
    UserRole.STUDENT -> listOf(
        TabItem("Trang chủ", Icons.Filled.Home, TabKind.STUDENT_HOME),
        TabItem("Tạo phiếu", Icons.Filled.Add, TabKind.CREATE_TICKET),
        TabItem("Lịch sử", Icons.Filled.DateRange, TabKind.STUDENT_HISTORY),
        TabItem("Cá nhân", Icons.Filled.Person, TabKind.PROFILE)
    )
    UserRole.ADMIN -> listOf(
        TabItem("Tổng quan", Icons.Filled.Home, TabKind.ADMIN_DASHBOARD),
        TabItem("Điều phối", Icons.Filled.Build, TabKind.ADMIN_DISPATCH),
        TabItem("Lịch sử", Icons.Filled.DateRange, TabKind.ADMIN_HISTORY),
        TabItem("Cá nhân", Icons.Filled.Person, TabKind.PROFILE)
    )
    UserRole.TECHNICIAN -> listOf(
        TabItem("Radar", Icons.Filled.Search, TabKind.TECH_RADAR),
        TabItem("Công việc", Icons.Filled.Build, TabKind.TECH_JOBS),
        TabItem("Lịch sử", Icons.Filled.DateRange, TabKind.TECH_HISTORY),
        TabItem("Cá nhân", Icons.Filled.Person, TabKind.PROFILE)
    )
}

@Composable
fun MainScaffold(
    profile: Profile,
    profileViewModel: ProfileViewModel,
    createTicketViewModel: CreateTicketViewModel,
    ticketHistoryViewModel: TicketHistoryViewModel,
    homeViewModel: HomeViewModel,
    adminDashboardViewModel: AdminDashboardViewModel,
    adminDispatchViewModel: AdminDispatchViewModel,
    adminHistoryViewModel: AdminHistoryViewModel,
    techJobsViewModel: TechJobsViewModel,
    radarViewModel: RadarViewModel,
    techHistoryViewModel: TechHistoryViewModel,
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
            when (tab.kind) {
                TabKind.PROFILE -> ProfileScreen(
                    profile = profile,
                    vm = profileViewModel,
                    onSignOut = onSignOut
                )
                TabKind.CREATE_TICKET -> CreateTicketScreen(vm = createTicketViewModel)
                TabKind.STUDENT_HISTORY -> TicketHistoryScreen(
                    userId = profile.id,
                    vm = ticketHistoryViewModel
                )
                TabKind.STUDENT_HOME -> HomeScreen(vm = homeViewModel)
                TabKind.ADMIN_DASHBOARD -> AdminDashboardScreen(vm = adminDashboardViewModel)
                TabKind.ADMIN_DISPATCH -> AdminDispatchScreen(vm = adminDispatchViewModel)
                TabKind.ADMIN_HISTORY -> AdminHistoryScreen(vm = adminHistoryViewModel)
                TabKind.TECH_JOBS -> TechJobsScreen(userId = profile.id, vm = techJobsViewModel)
                TabKind.TECH_RADAR -> RadarScreen(userId = profile.id, vm = radarViewModel)
                TabKind.TECH_HISTORY -> TechHistoryScreen(
                    userId = profile.id,
                    vm = techHistoryViewModel
                )
                TabKind.PLACEHOLDER -> Column(
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