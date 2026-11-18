package `in`.bharatwealth.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.pow
// Use this (Material 3):
import androidx.compose.material3.MaterialTheme

// NOT this (Material 2):
//import androidx.compose.material..materialIcon

private val inr = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
    maximumFractionDigits = 2
}
private fun money(v: Double) = inr.format(v)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BharatWealthApp() }
    }
}

data class Stock(val name: String, val symbol: String, val yahoo: String, val fallback: Double)
data class Quote(val price: Double, val change: Double, val history: List<Double>)

private val stocks = listOf(
    Stock("Reliance Industries", "RELIANCE", "RELIANCE.NS", 1280.0),
    Stock("HDFC Bank", "HDFCBANK", "HDFCBANK.NS", 980.0),
    Stock("Tata Consultancy Services", "TCS", "TCS.NS", 3200.0),
    Stock("Infosys", "INFY", "INFY.NS", 1480.0),
    Stock("ICICI Bank", "ICICIBANK", "ICICIBANK.NS", 1280.0),
    Stock("State Bank of India", "SBIN", "SBIN.NS", 790.0),
    Stock("ITC", "ITC", "ITC.NS", 410.0),
    Stock("Larsen & Toubro", "LT", "LT.NS", 3500.0)
)

data class Holding(val name: String, val units: Double, val avg: Double, val current: Double, val kind: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BharatWealthApp() {
    var dark by remember { mutableStateOf(true) }
    var tab by remember { mutableIntStateOf(0) }
    val holdings = remember {
        mutableStateListOf(
            Holding("Reliance Industries", 5.0, 1210.0, 1280.0, "Stock"),
            Holding("HDFC Bank", 8.0, 940.0, 980.0, "Stock"),
            Holding("Nifty 50 Index Fund", 120.0, 210.0, 226.0, "Mutual Fund")
        )
    }
    val scheme = if (dark) darkColorScheme(
        primary = androidx.compose.ui.graphics.Color(0xFF77E0B5),
        secondary = androidx.compose.ui.graphics.Color(0xFF8AB4F8),
        background = androidx.compose.ui.graphics.Color(0xFF0B1220),
        surface = androidx.compose.ui.graphics.Color(0xFF121D2E),
        surfaceVariant = androidx.compose.ui.graphics.Color(0xFF1D2A3D),
        onBackground = androidx.compose.ui.graphics.Color(0xFFE8EEF7),
        onSurface = androidx.compose.ui.graphics.Color(0xFFE8EEF7)
    ) else lightColorScheme(
        primary = androidx.compose.ui.graphics.Color(0xFF087E5B),
        secondary = androidx.compose.ui.graphics.Color(0xFF2866B2)
    )

    MaterialTheme(colorScheme = scheme) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Bharat Wealth", fontWeight = FontWeight.Bold)
                            Text("INDIAN MARKETS • NSE / BSE / MUTUAL FUNDS", fontSize = 9.sp, letterSpacing = 1.sp)
                        }
                    },
                    actions = {
                        IconButton(onClick = { dark = !dark }) {
                            Icon(if (dark) Icons.Default.LightMode else Icons.Default.DarkMode, "Toggle theme")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            },
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    val labels = listOf("Home", "Stocks", "Funds", "Portfolio", "SIP")
                    val icons = listOf(Icons.Default.Home, Icons.Default.ShowChart, Icons.Default.PieChart, Icons.Default.AccountBalanceWallet, Icons.Default.Calculate)
                    labels.forEachIndexed { i, label ->
                        NavigationBarItem(
                            selected = tab == i,
                            onClick = { tab = i },
                            icon = { Icon(icons[i], label) },
                            label = { Text(label, fontSize = 10.sp) }
                        )
                    }
                }
            }
        ) { padding ->
            when (tab) {
                0 -> HomeScreen(holdings, onNavigate = { tab = it }, Modifier.padding(padding))
                1 -> StocksScreen(Modifier.padding(padding))
                2 -> FundsScreen(Modifier.padding(padding))
                3 -> PortfolioScreen(holdings, Modifier.padding(padding))
                else -> SipScreen(Modifier.padding(padding))
            }
        }
    }
}

@Composable
fun PageColumn(modifier: Modifier = Modifier, content: LazyListScope.() -> Unit) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
}

@Composable
fun Panel(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content) }
}

@Composable
fun HomeScreen(holdings: List<Holding>, onNavigate: (Int) -> Unit, modifier: Modifier = Modifier) {
    val invested = holdings.sumOf { it.units * it.avg }
    val value = holdings.sumOf { it.units * it.current }
    val gain = value - invested
    PageColumn(modifier) {
        item {
            Panel {
                Text("TOTAL PORTFOLIO VALUE", color = MaterialTheme.colorScheme.onSurface.copy(.65f), fontSize = 11.sp, letterSpacing = 1.sp)
                Text(money(value), fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${if (gain >= 0) "+" else ""}${money(gain)}  (${if (invested > 0) gain / invested * 100 else 0.0}%)", color = if (gain >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text("Overall P&L", color = MaterialTheme.colorScheme.onSurface.copy(.6f), fontSize = 11.sp)
                }
                SimpleChart(listOf(22.0, 25.0, 23.0, 31.0, 28.0, 37.0, 35.0, 43.0, 41.0, 52.0))
            }
        }
        item {
            Text("MARKET SNAPSHOT", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MarketTile("NIFTY 50", "Connect data feed", "NSE")
                MarketTile("SENSEX", "Connect data feed", "BSE")
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Your holdings", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.weight(1f))
                Text("See all", color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { onNavigate(3) })
            }
        }
        items(holdings.take(3)) { HoldingRow(it) }
        item {
            Panel {
                Text("Explore", fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionChip("Stocks", Icons.Default.ShowChart) { onNavigate(1) }
                    ActionChip("Mutual funds", Icons.Default.PieChart) { onNavigate(2) }
                    ActionChip("SIP", Icons.Default.Calculate) { onNavigate(4) }
                }
            }
        }
        item { Text("Market prices are indicative. Verify with your broker before trading.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(.55f)) }
    }
}

@Composable
fun MarketTile(title: String, value: String, subtitle: String) {
    Panel {
        Text(title, fontWeight = FontWeight.Bold)
        Text(value, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
        Text(subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(.55f))
    }
}

@Composable
fun ActionChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    AssistChip(onClick = onClick, label = { Text(label, fontSize = 10.sp) }, leadingIcon = { Icon(icon, null, Modifier.size(16.dp)) })
}

@Composable
fun HoldingRow(h: Holding) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(38.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape), contentAlignment = Alignment.Center) {
            Text(h.name.take(1), fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(h.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text("${h.units} units • ${h.kind}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(.6f))
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(money(h.units * h.current), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            val pnl = (h.current - h.avg) * h.units
            Text("${if (pnl >= 0) "+" else ""}${money(pnl)}", color = if (pnl >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, fontSize = 11.sp)
        }
    }
}

@Composable
fun StocksScreen(modifier: Modifier = Modifier) {
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(stocks.first()) }
    var quote by remember { mutableStateOf<Quote?>(null) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Fetching latest available quote…") }
    val scope = rememberCoroutineScope()
    fun load(stock: Stock) {
        selected = stock
        loading = true
        message = "Fetching latest available quote…"
        scope.launch {
            val result = MarketApi.stockQuote(stock.yahoo, stock.fallback)
            quote = result
            message = if (result == null) "Live quote unavailable. Showing indicative sample data." else "Yahoo Finance • delayed / indicative data"
            loading = false
        }
    }
    LaunchedEffect(Unit) { load(selected) }
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Stocks", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Search NSE stocks") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true)
        val filtered = stocks.filter { it.name.contains(query, true) || it.symbol.contains(query, true) }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("1D", "1W", "1M", "1Y").forEach { AssistChip(onClick = {}, label = { Text(it) }) }
        }
        Panel {
            Text(selected.name, fontWeight = FontWeight.Bold)
            Text(selected.symbol + " • NSE", color = MaterialTheme.colorScheme.onSurface.copy(.6f), fontSize = 11.sp)
            Text(money(quote?.price ?: selected.fallback), fontSize = 29.sp, fontWeight = FontWeight.Bold)
            Text(if (quote != null) "${if (quote!!.change >= 0) "+" else ""}${"%.2f".format(quote!!.change)}% today" else "Indicative sample price", color = if ((quote?.change ?: 0.0) >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            SimpleChart(quote?.history ?: listOf(20.0, 22.0, 19.0, 25.0, 27.0, 24.0, 31.0, 29.0, 36.0))
            Text(if (loading) "Loading…" else message, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(.6f))
            OutlinedButton(onClick = { load(selected) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(6.dp)); Text("Refresh quote") }
        }
        Text("Popular NSE stocks", fontWeight = FontWeight.Bold)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(filtered) { s ->
                Row(Modifier.fillMaxWidth().clickable { load(s) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(s.name, fontWeight = FontWeight.SemiBold); Text(s.symbol, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(.6f)) }
                    Text(money(if (s == selected) quote?.price ?: s.fallback else s.fallback), fontWeight = FontWeight.SemiBold)
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun FundsScreen(modifier: Modifier = Modifier) {
    var search by remember { mutableStateOf("") }
    var funds by remember { mutableStateOf<List<Fund>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Search mutual funds by scheme name or AMFI code.") }
    val scope = rememberCoroutineScope()
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Mutual funds", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(search, { search = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("e.g. Parag Parikh Flexi Cap") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true)
        Button(onClick = {
            if (search.isNotBlank()) {
                loading = true
                scope.launch {
                    funds = MarketApi.searchFunds(search)
                    status = if (funds.isEmpty()) "No matching schemes found. Try a shorter name." else "${funds.size} matching schemes • NAV from MFAPI"
                    loading = false
                }
            }
        }, modifier = Modifier.fillMaxWidth()) { Text(if (loading) "Searching…" else "Search funds") }
        Text(status, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(.65f))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(funds) { f ->
                Panel {
                    Text(f.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Latest NAV", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(.6f))
                            Text("₹${"%.4f".format(f.nav)}", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                        Text("AMFI ${f.code}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(.6f))
                    }
                    Text("NAV date: ${f.date}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(.6f))
                }
            }
        }
    }
}

data class Fund(val code: String, val name: String, val nav: Double, val date: String)

@Composable
fun PortfolioScreen(holdings: List<Holding>, modifier: Modifier = Modifier) {
    var showAdd by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var units by remember { mutableStateOf("1") }
    var avg by remember { mutableStateOf("100") }
    var current by remember { mutableStateOf("100") }
    val invested = holdings.sumOf { it.units * it.avg }
    val value = holdings.sumOf { it.units * it.current }
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Portfolio", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { showAdd = true }) { Icon(Icons.Default.AddCircle, "Add holding") }
        }
        Panel {
            Text("Invested  ${money(invested)}", color = MaterialTheme.colorScheme.onSurface.copy(.7f), fontSize = 12.sp)
            Text("Current value  ${money(value)}", fontWeight = FontWeight.Bold, fontSize = 23.sp)
            Text("P&L  ${money(value - invested)}", color = if (value >= invested) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) { items(holdings) { HoldingRow(it) } }
    }
    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Add portfolio holding") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Stock / fund name") }, singleLine = true)
                    OutlinedTextField(units, { units = it }, label = { Text("Units") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    OutlinedTextField(avg, { avg = it }, label = { Text("Average buy price (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    OutlinedTextField(current, { current = it }, label = { Text("Current price / NAV (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val u = units.toDoubleOrNull(); val a = avg.toDoubleOrNull(); val c = current.toDoubleOrNull()
                    if (name.isNotBlank() && u != null && a != null && c != null && u > 0 && a >= 0 && c >= 0) {
                        (holdings as? androidx.compose.runtime.snapshots.SnapshotStateList<Holding>)?.add(Holding(name, u, a, c, "Manual"))
                        showAdd = false; name = ""
                    }
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun SipScreen(modifier: Modifier = Modifier) {
    var monthly by remember { mutableStateOf("5000") }
    var rate by remember { mutableStateOf("12") }
    var years by remember { mutableStateOf("10") }
    val p = monthly.toDoubleOrNull() ?: 0.0
    val r = (rate.toDoubleOrNull() ?: 0.0) / 1200.0
    val n = (years.toDoubleOrNull() ?: 0.0) * 12
    val invested = p * n
    val future = if (n > 0 && r > 0) p * (((1 + r).pow(n) - 1) / r) * (1 + r) else invested
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("SIP calculator", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Panel {
            InputMoney("Monthly investment (₹)", monthly) { monthly = it }
            InputMoney("Expected annual return (%)", rate) { rate = it }
            InputMoney("Investment period (years)", years) { years = it }
        }
        Panel {
            Text("ESTIMATED FUTURE VALUE", fontSize = 11.sp, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurface.copy(.6f))
            Text(money(future), fontSize = 29.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            HorizontalDivider()
            ResultLine("Amount invested", money(invested))
            ResultLine("Estimated returns", money(future - invested))
            ResultLine("Total value", money(future))
        }
        Text("Illustrative calculation only. Actual mutual fund returns are market-linked and not guaranteed.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(.6f))
    }
}

@Composable
fun InputMoney(label: String, value: String, onValue: (String) -> Unit) {
    OutlinedTextField(value, onValue, modifier = Modifier.fillMaxWidth(), label = { Text(label) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
}
@Composable
fun ResultLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = MaterialTheme.colorScheme.onSurface.copy(.7f)); Text(value, fontWeight = FontWeight.SemiBold) }
}

@Composable // Ensure your function is marked as @Composable
fun SimpleChart(values: List<Double>) {
    // 1. Extract the color outside the Canvas block
    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(Modifier.fillMaxWidth().height(112.dp)) {
        if (values.size < 2) return@Canvas
        val min = values.minOrNull() ?: 0.0
        val max = values.maxOrNull() ?: 1.0
        val range = (max - min).takeIf { it > 0 } ?: 1.0
        val path = Path()

        values.forEachIndexed { i, v ->
            val x = size.width * i / (values.size - 1)
            val y = size.height - ((v - min) / range).toFloat() * (size.height * .82f) - size.height * .08f
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        // 2. Use the extracted variable here
        drawPath(
            path = path,
            color = primaryColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}


object MarketApi {
    private val client = OkHttpClient()
    private suspend fun get(url: String): String? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0 BharatWealth/1.0").build()
            client.newCall(req).execute().use { if (it.isSuccessful) it.body?.string() else null }
        } catch (_: Exception) { null }
    }

    suspend fun stockQuote(symbol: String, fallback: Double): Quote? {
        val raw = get("https://query1.finance.yahoo.com/v8/finance/chart/$symbol?range=1mo&interval=1d") ?: return null
        return try {
            val result = JSONObject(raw).getJSONObject("chart").getJSONArray("result").getJSONObject(0)
            val meta = result.getJSONObject("meta")
            val price = meta.optDouble("regularMarketPrice", fallback)
            val previous = meta.optDouble("chartPreviousClose", price)
            val arr = result.getJSONObject("indicators").getJSONArray("quote").getJSONObject(0).getJSONArray("close")
            val points = (0 until arr.length()).mapNotNull { if (arr.isNull(it)) null else arr.optDouble(it) }
            Quote(price, if (previous != 0.0) (price - previous) / previous * 100 else 0.0, points.ifEmpty { listOf(fallback) })
        } catch (_: Exception) { null }
    }

    suspend fun searchFunds(query: String): List<Fund> {
        val raw = get("https://api.mfapi.in/mf/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}") ?: return emptyList()
        return try {
            val arr = org.json.JSONArray(raw)
            (0 until minOf(arr.length(), 15)).mapNotNull { i ->
                val item = arr.getJSONObject(i)
                val code = item.optString("schemeCode")
                val name = item.optString("schemeName")
                val detail = get("https://api.mfapi.in/mf/$code") ?: return@mapNotNull null
                val obj = JSONObject(detail)
                val data = obj.optJSONArray("data") ?: return@mapNotNull null
                if (data.length() == 0) return@mapNotNull null
                val latest = data.getJSONObject(0)
                Fund(code, name, latest.optString("nav").toDoubleOrNull() ?: return@mapNotNull null, latest.optString("date"))
            }
        } catch (_: Exception) { emptyList() }
    }
}
