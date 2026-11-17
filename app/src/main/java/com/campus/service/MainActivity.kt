package com.campus.service

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.campus.service.ui.admin.AdminDashboardViewModel
import com.campus.service.ui.admin.AdminDispatchViewModel
import com.campus.service.ui.admin.AdminHistoryViewModel
import com.campus.service.ui.auth.AuthScreen
import com.campus.service.ui.auth.AuthViewModel
import com.campus.service.ui.navigation.MainScaffold
import com.campus.service.ui.notification.NotificationViewModel
import com.campus.service.ui.profile.ProfileViewModel
import com.campus.service.ui.student.CreateTicketViewModel
import com.campus.service.ui.student.HomeViewModel
import com.campus.service.ui.student.TicketHistoryViewModel
import com.campus.service.ui.technician.RadarViewModel
import com.campus.service.ui.technician.TechHistoryViewModel
import com.campus.service.ui.technician.TechJobsViewModel
import com.campus.service.ui.theme.CampusTheme

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private val profileViewModel: ProfileViewModel by viewModels()
    private val createTicketViewModel: CreateTicketViewModel by viewModels()
    private val ticketHistoryViewModel: TicketHistoryViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()
    private val adminDashboardViewModel: AdminDashboardViewModel by viewModels()
    private val adminDispatchViewModel: AdminDispatchViewModel by viewModels()
    private val adminHistoryViewModel: AdminHistoryViewModel by viewModels()
    private val techJobsViewModel: TechJobsViewModel by viewModels()
    private val radarViewModel: RadarViewModel by viewModels()
    private val techHistoryViewModel: TechHistoryViewModel by viewModels()
    private val notificationViewModel: NotificationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Edge-to-edge: thanh trạng thái/điều hướng trong suốt, biểu tượng tự đổi
        // sáng/tối theo chế độ của hệ thống (khớp với CampusTheme dùng isSystemInDarkTheme)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            CampusTheme {
                // Nền toàn màn hình theo giao diện (cần cho chế độ tối ở màn Đăng nhập)
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val state by authViewModel.state.collectAsState()
                    val profile = state.profile

                    when {
                        state.initializing -> Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                        // MainScaffold tự xử lý inset qua Scaffold
                        profile != null -> MainScaffold(
                            profile = profile,
                            profileViewModel = profileViewModel,
                            createTicketViewModel = createTicketViewModel,
                            ticketHistoryViewModel = ticketHistoryViewModel,
                            homeViewModel = homeViewModel,
                            adminDashboardViewModel = adminDashboardViewModel,
                            adminDispatchViewModel = adminDispatchViewModel,
                            adminHistoryViewModel = adminHistoryViewModel,
                            techJobsViewModel = techJobsViewModel,
                            radarViewModel = radarViewModel,
                            techHistoryViewModel = techHistoryViewModel,
                            notificationViewModel = notificationViewModel,
                            onSignOut = authViewModel::signOut
                        )
                        // Màn Đăng nhập không có Scaffold nên tự chừa chỗ cho thanh hệ thống
                        else -> Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .navigationBarsPadding()
                        ) {
                            AuthScreen(state = state, vm = authViewModel)
                        }
                    }
                }
            }
        }
    }
}