package com.example.pundarapp.ui.screens.home

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pundarapp.ui.components.*
import com.example.pundarapp.ui.data.AppState
import com.example.pundarapp.ui.theme.*

// ── Network definitions ───────────────────────────────────────────
data class MobileNetwork(
    val id:       String,
    val name:     String,
    val color:    Color,
    val prefixes: List<String>
)

private val NETWORKS = listOf(
    MobileNetwork("globe","Globe",Blue400,listOf("0817","0904","0905","0906","0915","0916","0917","0926","0927","0935","0936","0937","0945","0953","0954","0955","0956","0965","0966","0967","0975","0977","0978","0979","0995","0996","0997")),
    MobileNetwork("tm","TM (Touch Mobile)",Color(0xFF0077CC),listOf("0817","0904","0905","0906","0915","0916","0926","0927","0935","0936","0937","0945","0955","0956","0965","0966","0967","0975","0976")),
    MobileNetwork("smart","Smart",Green400,listOf("0813","0907","0908","0909","0910","0911","0912","0913","0914","0918","0919","0920","0921","0928","0929","0939","0946","0947","0948","0949","0950","0989","0998","0999")),
    MobileNetwork("tnt","TNT (Talk N Text)",Orange500,listOf("0907","0908","0909","0910","0912","0918","0919","0920","0921","0928","0929","0930","0938","0939","0946","0947","0948","0949","0950","0989","0998","0999")),
    MobileNetwork("dito","DITO",Color(0xFF8B5CF6),listOf("0895","0896","0897","0898","0991","0992","0993","0994"))
)

// ── Load product types & model ────────────────────────────────────
enum class LoadProductType { PREPAID, POSTPAID }

data class LoadProduct(
    val id:          String,
    val name:        String,
    val amount:      Double,
    val description: String,
    val validity:    String,
    val type:        LoadProductType
)

private fun pp(id: String, name: String, amount: Double, desc: String, validity: String) =
    LoadProduct(id, name, amount, desc, validity, LoadProductType.PREPAID)

private val LOAD_PRODUCTS: Map<String, List<LoadProduct>> = mapOf(
    "globe" to listOf(
        pp("g1","GoSURF50",    50.0, "1GB data + unlimited texts",   "3 days"),
        pp("g2","GoSURF99",    99.0, "3GB data + unli calls/texts",  "7 days"),
        pp("g3","GoSAKTO",     10.0, "100MB data",                   "1 day"),
        pp("g4","GoUNLI",      20.0, "Unlimited texts to Globe/TM",  "1 day"),
        pp("g5","GoALL",      299.0, "8GB data + unli all net calls","30 days"),
        pp("g6","GoLIVE100",  100.0, "2GB data + unli texts",        "7 days"),
        pp("g7","Regular Load",200.0,"Regular airtime load",         "—"),
        pp("g8","Regular Load",300.0,"Regular airtime load",         "—"),
        pp("g9","Regular Load",500.0,"Regular airtime load",         "—"),
        pp("g0","Regular Load",1000.0,"Regular airtime load",        "—")),
    "tm" to listOf(
        pp("t1","TMo50",       50.0, "1GB data + unli texts to Globe/TM","3 days"),
        pp("t2","GMADATA10",   10.0, "100MB data",                   "1 day"),
        pp("t3","GOUNLI20",    20.0, "Unli texts to all networks",   "1 day"),
        pp("t4","TMoSURF99",   99.0, "2GB data",                     "7 days"),
        pp("t5","Regular Load",100.0,"Regular airtime load",         "—"),
        pp("t6","Regular Load",200.0,"Regular airtime load",         "—"),
        pp("t7","Regular Load",300.0,"Regular airtime load",         "—"),
        pp("t8","Regular Load",500.0,"Regular airtime load",         "—")),
    "smart" to listOf(
        pp("s1","GigaLife50",  50.0, "1GB data + unli texts to Smart/TNT","3 days"),
        pp("s2","GigaLife99",  99.0, "3GB + unli calls/texts",       "7 days"),
        pp("s3","GIGA100",    100.0, "4GB open access",              "7 days"),
        pp("s4","BigByte10",   10.0, "100MB data",                   "1 day"),
        pp("s5","GigaSurf299",299.0, "10GB + unli all net",          "30 days"),
        pp("s6","Regular Load",200.0,"Regular airtime load",         "—"),
        pp("s7","Regular Load",500.0,"Regular airtime load",         "—"),
        pp("s8","Regular Load",1000.0,"Regular airtime load",        "—")),
    "tnt" to listOf(
        pp("n1","TNTSurf50",   50.0, "1.5GB data",                   "3 days"),
        pp("n2","TNT10",       10.0, "Basic load",                   "—"),
        pp("n3","TrickyUNLI20",20.0, "Unli texts to TNT/Smart",      "1 day"),
        pp("n4","GIGA100",    100.0, "4GB data",                     "7 days"),
        pp("n5","Regular Load",200.0,"Regular airtime load",         "—"),
        pp("n6","Regular Load",500.0,"Regular airtime load",         "—")),
    "dito" to listOf(
        pp("d1","DITO30",      50.0, "5GB data",                     "7 days"),
        pp("d2","DITO10",      10.0, "1GB data",                     "1 day"),
        pp("d3","DITO99",      99.0, "15GB data",                    "30 days"),
        pp("d4","DITO299",    299.0, "50GB data",                    "30 days"),
        pp("d5","Regular Load",100.0,"Regular airtime load",         "—"),
        pp("d6","Regular Load",200.0,"Regular airtime load",         "—"),
        pp("d7","Regular Load",300.0,"Regular airtime load",         "—"),
        pp("d8","Regular Load",500.0,"Regular airtime load",         "—"))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyLoadScreen(navController: NavController) {
    var selectedNetwork by remember { mutableStateOf<MobileNetwork?>(null) }
    var phoneNumber     by remember { mutableStateOf("") }
    var selectedProduct by remember { mutableStateOf<LoadProduct?>(null) }
    var prefixError     by remember { mutableStateOf<String?>(null) }
    var showStep2       by remember { mutableStateOf(false) }
    var selectedType    by remember { mutableStateOf(LoadProductType.PREPAID) }
    val context         = LocalContext.current

    fun detectNetwork(number: String): MobileNetwork? {
        if (number.length < 4) return null
        val prefix = number.take(4)
        return NETWORKS.find { it.prefixes.contains(prefix) }
    }

    fun validatePrefix(): Boolean {
        if (phoneNumber.length < 4) return true
        val detected = detectNetwork(phoneNumber)
        return if (selectedNetwork != null && detected != null && detected.id != selectedNetwork!!.id) {
            prefixError = "Number doesn't match ${selectedNetwork!!.name}. Detected: ${detected.name}."
            false
        } else {
            prefixError = null
            true
        }
    }

    Scaffold(
        topBar = {
            PundarDetailTopBar(
                title  = "Buy Load",
                onBack = {
                    if (showStep2) showStep2 = false
                    else navController.navigateUp()
                }
            )
        },
        containerColor = Navy900,
        bottomBar = {
            if (showStep2) {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    PundarPrimaryButton(
                        text    = "Buy Load",
                        enabled = phoneNumber.length == 11 && selectedProduct != null
                                  && prefixError == null && selectedType == LoadProductType.PREPAID,
                        onClick = {
                            if (!validatePrefix()) return@PundarPrimaryButton
                            val amt = selectedProduct?.amount ?: 0.0
                            if (amt > AppState.walletBalance.value) {
                                Toast.makeText(context, "Insufficient balance.", Toast.LENGTH_SHORT).show()
                                return@PundarPrimaryButton
                            }
                            Toast.makeText(context,
                                "Load sent to $phoneNumber! ✓", Toast.LENGTH_SHORT).show()
                            navController.navigateUp()
                        }
                    )
                }
            }
        }
    ) { padding ->
        AnimatedContent(
            targetState = showStep2,
            transitionSpec = {
                if (targetState)
                    slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                else
                    slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
            },
            label = "loadStep",
            modifier = Modifier.fillMaxSize().padding(padding)
        ) { onStep2 ->
            if (!onStep2) {
                // ── Step 1: Network selector ──────────────────────
                LazyColumn(
                    contentPadding      = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text("Select SIM Network",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold, color = TextWhite)
                        Spacer(Modifier.height(4.dp))
                        Text("Choose your SIM provider to continue.",
                            style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        Spacer(Modifier.height(8.dp))
                    }
                    items(NETWORKS.size) { i ->
                        val net = NETWORKS[i]
                        NetworkCard(
                            network  = net,
                            selected = selectedNetwork?.id == net.id,
                            onClick  = {
                                selectedNetwork = net
                                selectedProduct = null
                                selectedType    = LoadProductType.PREPAID
                                showStep2       = true
                            }
                        )
                    }
                }
            } else {
                // ── Step 2: Number + load product ─────────────────
                val net      = selectedNetwork!!
                val tabTitles = listOf("PREPAID", "POSTPAID")
                val products = (LOAD_PRODUCTS[net.id] ?: emptyList())
                    .filter { it.type == selectedType }

                LazyColumn(
                    contentPadding      = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Network badge
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(net.color.copy(0.10f))
                                .border(1.dp, net.color.copy(0.30f), RoundedCornerShape(14.dp))
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(36.dp).clip(CircleShape)
                                    .background(net.color.copy(0.20f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.SignalCellularAlt, null,
                                    tint = net.color, modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(net.name, fontWeight = FontWeight.SemiBold,
                                    color = TextWhite,
                                    style = MaterialTheme.typography.titleSmall)
                                Text("Tap to change",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                    modifier = Modifier.clickable { showStep2 = false })
                            }
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Filled.CheckCircle, null,
                                tint = net.color, modifier = Modifier.size(18.dp))
                        }
                    }

                    // Phone number
                    item {
                        Text("Mobile Number",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSoft, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value         = phoneNumber,
                            onValueChange = {
                                if (it.length <= 11 && it.all(Char::isDigit)) {
                                    phoneNumber = it
                                    validatePrefix()
                                    val detected = detectNetwork(it)
                                    if (detected != null && detected.id != net.id) {
                                        prefixError = "This looks like a ${detected.name} number."
                                    }
                                }
                            },
                            placeholder     = { Text("09XXXXXXXXX", color = TextDim) },
                            leadingIcon     = { Icon(Icons.Filled.PhoneAndroid, null, tint = TextMuted) },
                            isError         = prefixError != null,
                            supportingText  = if (prefixError != null) ({
                                Text(prefixError!!, color = Orange500)
                            }) else null,
                            modifier        = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            colors          = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor      = net.color,
                                unfocusedBorderColor    = NavyBorder,
                                focusedContainerColor   = Navy700,
                                unfocusedContainerColor = Navy700,
                                focusedTextColor        = TextWhite,
                                unfocusedTextColor      = TextWhite,
                                cursorColor             = net.color,
                                errorBorderColor        = Orange500
                            ),
                            shape      = RoundedCornerShape(14.dp),
                            singleLine = true
                        )
                    }

                    // PREPAID / POSTPAID tabs
                    item {
                        TabRow(
                            selectedTabIndex = selectedType.ordinal,
                            containerColor   = Navy800,
                            contentColor     = net.color
                        ) {
                            tabTitles.forEachIndexed { i, title ->
                                val t = LoadProductType.entries[i]
                                Tab(
                                    selected = selectedType == t,
                                    onClick  = { selectedType = t; selectedProduct = null },
                                    selectedContentColor   = net.color,
                                    unselectedContentColor = TextMuted,
                                    text = {
                                        Text(title, fontSize = 13.sp,
                                            fontWeight = if (selectedType == t) FontWeight.Bold else FontWeight.Normal)
                                    }
                                )
                            }
                        }
                    }

                    // Products or Coming Soon
                    if (selectedType == LoadProductType.POSTPAID) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Navy700)
                                    .border(1.dp, NavyBorder, RoundedCornerShape(14.dp))
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Filled.Schedule, null,
                                        tint = TextMuted, modifier = Modifier.size(40.dp))
                                    Spacer(Modifier.height(12.dp))
                                    Text("Coming Soon",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize   = 16.sp,
                                        color      = TextSoft)
                                    Spacer(Modifier.height(4.dp))
                                    Text("Postpaid load top-up is not yet available.",
                                        style     = MaterialTheme.typography.bodySmall,
                                        color     = TextMuted,
                                        textAlign = TextAlign.Center,
                                        modifier  = Modifier.padding(horizontal = 24.dp))
                                }
                            }
                        }
                    } else {
                        items(products.size) { i ->
                            val product   = products[i]
                            val isSelected = product.id == selectedProduct?.id
                            ProductCard(
                                product    = product,
                                netColor   = net.color,
                                isSelected = isSelected,
                                onClick    = { selectedProduct = product }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Product card ──────────────────────────────────────────────────
@Composable
private fun ProductCard(
    product:    LoadProduct,
    netColor:   Color,
    isSelected: Boolean,
    onClick:    () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) netColor.copy(0.14f) else Navy700)
            .border(
                1.dp,
                if (isSelected) netColor.copy(0.55f) else NavyBorder,
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(product.name,
                fontWeight = FontWeight.Bold,
                fontSize   = 15.sp,
                color      = if (isSelected) netColor else TextWhite)
            Spacer(Modifier.height(2.dp))
            Text(product.description,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted)
            if (product.validity != "—") {
                Text("Valid for ${product.validity}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextDim)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text("₱${"%.0f".format(product.amount)}",
                fontWeight = FontWeight.ExtraBold,
                fontSize   = 20.sp,
                color      = if (isSelected) netColor else TextSoft)
            if (isSelected) {
                Icon(Icons.Filled.CheckCircle, null,
                    tint     = netColor,
                    modifier = Modifier.size(16.dp).align(Alignment.CenterHorizontally))
            }
        }
    }
}

// ── Network card ──────────────────────────────────────────────────
@Composable
private fun NetworkCard(
    network:  MobileNetwork,
    selected: Boolean,
    onClick:  () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (selected)
                    Modifier.background(network.color.copy(0.12f))
                else
                    Modifier.background(Brush.linearGradient(listOf(Navy800, Navy700)))
            )
            .border(
                1.dp,
                if (selected) network.color.copy(0.50f) else Glass10,
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(44.dp).clip(CircleShape)
                .background(network.color.copy(0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.SignalCellularAlt, null,
                tint = network.color, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(network.name,
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color      = TextWhite,
            modifier   = Modifier.weight(1f))
        if (selected) {
            Icon(Icons.Filled.CheckCircle, null, tint = network.color)
        } else {
            Icon(Icons.Filled.ChevronRight, null, tint = TextMuted)
        }
    }
}
