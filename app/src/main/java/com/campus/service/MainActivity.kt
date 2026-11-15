package com.campus.service

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.campus.service.ui.profile.ProfileViewModel
import com.campus.service.ui.student.CreateTicketViewModel
import com.campus.service.ui.student.HomeViewModel
import com.campus.service.ui.student.TicketHistoryViewModel
import com.campus.service.ui.technician.TechJobsViewModel

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val state by authViewModel.state.collectAsState()
                val profile = state.profile

                when {
                    state.initializing -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
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
                        onSignOut = authViewModel::signOut
                    )
                    else -> AuthScreen(state = state, vm = authViewModel)
                }
            }
        }
    }
}