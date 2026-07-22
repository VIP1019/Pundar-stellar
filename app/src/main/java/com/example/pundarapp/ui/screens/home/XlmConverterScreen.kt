package com.example.pundarapp.ui.screens.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pundarapp.data.remote.CurrencyRepository
import com.example.pundarapp.data.remote.CurrencyData
import com.example.pundarapp.data.remote.supportedCurrencies
import com.example.pundarapp.ui.components.AnimatedBackground
import com.example.pundarapp.ui.components.BgAccent
import com.example.pundarapp.ui.components.PundarCard
import com.example.pundarapp.ui.components.PundarDetailTopBar
import com.example.pundarapp.ui.data.AppState
import com.example.pundarapp.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XlmConverterScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    var xlmAmountText by remember { mutableStateOf(AppState.walletBalance.value.toString()) }
    var rates by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var lastUpdated by remember { mutableStateOf<Long>(0) }
    
    val pullState = rememberPullToRefreshState()

    suspend fun fetchRates(force: Boolean = false) {
        val result = CurrencyRepository.getXlmRates(force)
        if (result.isNotEmpty()) {
            rates = result
            lastUpdated = CurrencyRepository.getLastFetchTime()
        } else if (force) {
            Toast.makeText(context, "Failed to update rates. Using cached.", Toast.LENGTH_SHORT).show()
        }
    }

    // Initial fetch and auto-refresh every 60s
    LaunchedEffect(Unit) {
        fetchRates(force = false)
        isLoading = false
        while(true) {
            delay(60_000)
            fetchRates(force = false)
        }
    }

    AnimatedBackground(accent = BgAccent.Purple) {
        Scaffold(
            topBar = {
                PundarDetailTopBar(
                    title = "XLM Converter",
                    onBack = { navController.navigateUp() },
                    actions = {
                        IconButton(onClick = {
                            scope.launch {
                                isRefreshing = true
                                fetchRates(force = true)
                                isRefreshing = false
                            }
                        }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = PundarTextPrimary)
                        }
                    }
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    scope.launch {
                        fetchRates(force = true)
                        isRefreshing = false
                    }
                },
                state = pullState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Input Card
                    PundarCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CurrencyExchange,
                                contentDescription = null,
                                tint = PundarGold,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            
                            Text(
                                text = "Enter XLM Amount",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PundarTextSecondary
                            )
                            Spacer(Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = xlmAmountText,
                                onValueChange = { newValue ->
                                    if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                                        xlmAmountText = newValue
                                    }
                                },
                                textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PundarBlue,
                                    unfocusedBorderColor = PundarBorder,
                                    focusedContainerColor = PundarSurface,
                                    unfocusedContainerColor = PundarSurface,
                                    focusedTextColor = PundarTextPrimary,
                                    unfocusedTextColor = PundarTextPrimary
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                suffix = {
                                    Text("XLM", color = PundarTextSecondary, style = MaterialTheme.typography.titleMedium)
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Current Balance: ${String.format("%,.2f XLM", AppState.walletBalance.value)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PundarTextTertiary
                                )
                                TextButton(
                                    onClick = { xlmAmountText = AppState.walletBalance.value.toString() },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Use Max", color = PundarBlue, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }

                    // Last updated indicator
                    if (lastUpdated > 0) {
                        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
                        Text(
                            text = "Rates updated: ${sdf.format(Date(lastUpdated))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = PundarTextTertiary,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }

                    // Conversions Grid
                    if (isLoading && rates.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = PundarBlue)
                        }
                    } else {
                        val xlmAmount = xlmAmountText.toDoubleOrNull() ?: 0.0
                        
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(supportedCurrencies) { currency ->
                                val rate = rates[currency.code] ?: 0.0
                                val convertedValue = xlmAmount * rate
                                
                                PundarCard {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = currency.flag,
                                                fontSize = 24.sp
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(PundarBlue.copy(alpha = 0.2f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = currency.code,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = PundarBlue,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        
                                        Spacer(Modifier.height(12.dp))
                                        
                                        Text(
                                            text = currency.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = PundarTextSecondary
                                        )
                                        
                                        Spacer(Modifier.height(4.dp))
                                        
                                        Text(
                                            text = "${currency.symbol}${if (convertedValue > 0) String.format("%,.2f", convertedValue) else "0.00"}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = PundarTextPrimary
                                        )
                                        
                                        Spacer(Modifier.height(8.dp))
                                        
                                        Text(
                                            text = "1 XLM = ${currency.symbol}${if (rate > 0) String.format("%.4f", rate) else "0.0000"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = PundarTextTertiary
                                        )
                                    }
                                }
                            }
                            
                            // Branding item
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "Powered by",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PundarTextTertiary
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "CoinGecko & Stellar",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = PundarTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
