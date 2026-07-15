package com.example.pundarapp.ui.screens.circle

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

@Composable
fun CircleAdminScreen(navController: NavController, circleId: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Circle Admin Screen Placeholder for $circleId")
    }
}

@Composable
fun CircleInviteMethodsScreen(navController: NavController, circleId: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Invite Methods Screen Placeholder for $circleId")
    }
}
