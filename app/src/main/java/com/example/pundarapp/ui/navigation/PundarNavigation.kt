package com.example.pundarapp.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.pundarapp.ui.components.PundarBottomBar
import com.example.pundarapp.ui.screens.auth.LoginScreen
import com.example.pundarapp.ui.screens.auth.RegisterScreen
import com.example.pundarapp.ui.screens.circle.CircleDetailScreen
import com.example.pundarapp.ui.screens.circle.CircleInviteScreen
import com.example.pundarapp.ui.screens.circle.CircleScreen
import com.example.pundarapp.ui.screens.circle.CreateCircleScreen
import com.example.pundarapp.ui.screens.grow.GrowScreen
import com.example.pundarapp.ui.screens.grow.StockDetailScreen
import com.example.pundarapp.ui.screens.home.HomeScreen
import com.example.pundarapp.ui.screens.home.NotificationScreen
import com.example.pundarapp.ui.screens.home.QrSendConfirmScreen
import com.example.pundarapp.ui.screens.home.ReceiveMoneyScreen
import com.example.pundarapp.ui.screens.home.ScanScreen
import com.example.pundarapp.ui.screens.home.UserSettingsScreen
import com.example.pundarapp.ui.screens.home.ChangeMpinScreen
import com.example.pundarapp.ui.screens.pay.BillDetailScreen
import com.example.pundarapp.ui.screens.pay.NewGroupBillScreen
import com.example.pundarapp.ui.screens.pay.PayScreen
import com.example.pundarapp.ui.screens.pay.BillQrScreen
import com.example.pundarapp.ui.screens.pay.InstantSettlementScreen
import com.example.pundarapp.ui.screens.home.SendMoneyScreen
import com.example.pundarapp.ui.screens.home.BuyLoadScreen
import com.example.pundarapp.ui.screens.SplashScreen

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val PAY = "pay"
    const val PAY_NEW_BILL = "pay/new"
    const val BILL_DETAIL = "pay/bill/{billId}"
    const val BILL_QR = "pay/bill/{billId}/qr"
    const val INSTANT_SETTLE = "pay/instant_settle"
    const val CIRCLE = "circle"
    const val CIRCLE_CREATE = "circle/create"
    const val CIRCLE_DETAIL = "circle/{circleId}"
    const val CIRCLE_INVITE = "circle/invite/{inviteId}"
    const val GROW = "grow"
    const val STOCK_DETAIL = "grow/stock/{ticker}"
    fun stockDetail(ticker: String) = "grow/stock/$ticker"
    fun billDetail(billId: String) = "pay/bill/$billId"
    fun billQr(billId: String) = "pay/bill/$billId/qr"
    fun circleDetail(circleId: String) = "circle/$circleId"
    fun circleInvite(inviteId: String) = "circle/invite/$inviteId"
    const val SETTINGS = "settings"
    const val NOTIFICATIONS = "notifications"
    const val SCAN = "scan"
    const val CHANGE_MPIN = "change_mpin"
    const val SEND_MONEY = "send_money"
    const val RECEIVE_MONEY = "receive_money"
    const val QR_SEND_CONFIRM = "qr_send_confirm"
    const val BUY_LOAD = "buy_load"
}

// Routes where bottom bar should be visible
private val bottomBarRoutes = setOf(Routes.HOME, Routes.PAY, Routes.SCAN, Routes.CIRCLE, Routes.GROW)

@Composable
fun PundarNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Routes.LOGIN

    val showBottomBar = currentRoute in bottomBarRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                PundarBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        if (route != currentRoute) {
                            navController.navigate(route) {
                                popUpTo(Routes.HOME) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Routes.SPLASH) {
                SplashScreen(
                    onSplashFinished = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.LOGIN) {
                LoginScreen(navController = navController)
            }
            composable(Routes.REGISTER) {
                RegisterScreen(navController = navController)
            }
            composable(Routes.HOME) {
                HomeScreen(navController = navController)
            }
            composable(Routes.SETTINGS) {
                UserSettingsScreen(navController = navController)
            }
            composable(Routes.CHANGE_MPIN) {
                ChangeMpinScreen(navController = navController)
            }
            composable(Routes.NOTIFICATIONS) {
                NotificationScreen(navController = navController)
            }
            composable(Routes.PAY) {
                PayScreen(navController = navController)
            }
            composable(Routes.SCAN) {
                ScanScreen(navController = navController)
            }
            composable(Routes.RECEIVE_MONEY) {
                ReceiveMoneyScreen(navController = navController)
            }
            composable(Routes.QR_SEND_CONFIRM) {
                QrSendConfirmScreen(navController = navController)
            }
            composable(Routes.PAY_NEW_BILL) {
                NewGroupBillScreen(navController = navController)
            }
            composable(Routes.INSTANT_SETTLE) {
                InstantSettlementScreen(navController = navController)
            }
            composable(
                route = Routes.BILL_DETAIL,
                arguments = listOf(navArgument("billId") { type = NavType.StringType })
            ) { backStackEntry ->
                val billId = backStackEntry.arguments?.getString("billId") ?: ""
                BillDetailScreen(billId = billId, navController = navController)
            }
            composable(
                route = Routes.BILL_QR,
                arguments = listOf(navArgument("billId") { type = NavType.StringType })
            ) { backStackEntry ->
                val billId = backStackEntry.arguments?.getString("billId") ?: ""
                BillQrScreen(billId = billId, navController = navController)
            }
            composable(Routes.CIRCLE) {
                CircleScreen(navController = navController)
            }
            composable(Routes.CIRCLE_CREATE) {
                CreateCircleScreen(navController = navController)
            }
            composable(
                route = Routes.CIRCLE_DETAIL,
                arguments = listOf(navArgument("circleId") { type = NavType.StringType })
            ) { backStackEntry ->
                val circleId = backStackEntry.arguments?.getString("circleId") ?: ""
                CircleDetailScreen(circleId = circleId, navController = navController)
            }
            composable(
                route = Routes.CIRCLE_INVITE,
                arguments = listOf(navArgument("inviteId") { type = NavType.StringType })
            ) { backStackEntry ->
                val inviteId = backStackEntry.arguments?.getString("inviteId") ?: ""
                CircleInviteScreen(inviteId = inviteId, navController = navController)
            }
            composable(Routes.GROW) {
                GrowScreen(navController = navController)
            }
            composable(Routes.STOCK_DETAIL, arguments = listOf(navArgument("ticker") { type = NavType.StringType })) { backStackEntry ->
                val ticker = backStackEntry.arguments?.getString("ticker") ?: ""
                StockDetailScreen(ticker = ticker, navController = navController)
            }
            composable(Routes.SEND_MONEY) {
                SendMoneyScreen(navController = navController)
            }
            composable(Routes.BUY_LOAD) {
                BuyLoadScreen(navController = navController)
            }
        }
    }
}
