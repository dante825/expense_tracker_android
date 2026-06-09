package com.kangwei.expensetracker.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kangwei.expensetracker.data.db.relation.ExpenseWithDetails
import com.kangwei.expensetracker.ui.components.*
import com.kangwei.expensetracker.ui.components.LocalCurrencyCode
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

@Composable
fun DashboardScreen(innerPadding: PaddingValues, vm: DashboardViewModel = viewModel()) {
    LaunchedEffect(Unit) { vm.seed() }

    val expenses by vm.filteredExpenses.collectAsStateWithLifecycle()
    val allExpenses by vm.allExpenses.collectAsStateWithLifecycle()
    val tags by vm.allTags.collectAsStateWithLifecycle()
    val period by vm.selectedPeriod.collectAsStateWithLifecycle()
    val selectedTagId by vm.selectedTagId.collectAsStateWithLifecycle()

    val currencyCode = LocalCurrencyCode.current
    val income = expenses.filter { it.expense.isIncome }.sumOf { it.expense.amount }
    val spent = expenses.filter { !it.expense.isIncome }.sumOf { it.expense.amount }
    val net = income - spent

    val categoryTotals = expenses
        .filter { !it.expense.isIncome }
        .groupBy { it.category?.name ?: "Other" }
        .mapValues { e -> e.value.sumOf { it.expense.amount } }
        .entries.sortedByDescending { it.value }
        .map { CategorySlice(it.key, it.value) }

    LazyColumn(contentPadding = innerPadding) {
        // Period selector
        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                TimePeriod.entries.forEachIndexed { index, tp ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index, TimePeriod.entries.size),
                        onClick = { vm.selectedPeriod.value = tp },
                        selected = period == tp
                    ) { Text(tp.label) }
                }
            }
        }

        // Tag filter chips
        if (tags.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TagChip("All", selectedTagId == null) { vm.selectedTagId.value = null }
                    tags.forEach { tag ->
                        TagChip(tag.name, selectedTagId == tag.id) {
                            vm.selectedTagId.value = if (selectedTagId == tag.id) null else tag.id
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        // Summary cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SummaryCard("Income", income, Color(0xFF2E7D32), Modifier.weight(1f))
                SummaryCard("Spent", spent, Color(0xFFC62828), Modifier.weight(1f))
                SummaryCard("Net", net, if (net >= 0) Color(0xFF1565C0) else Color(0xFFE65100), Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
        }

        // Stats row
        item {
            val spends = expenses.filter { !it.expense.isIncome }
            val avg = if (spends.isEmpty()) 0.0 else spends.sumOf { it.expense.amount } / spends.size
            val topCat = categoryTotals.firstOrNull()?.name ?: "—"
            val savingsRate = if (income > 0) (net / income * 100) else null
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StatTile("Transactions", "${expenses.size}", Modifier.weight(1f))
                StatTile("Avg / entry", if (avg > 0) formatCurrency(avg, currencyCode) else "—", Modifier.weight(1f))
                if (savingsRate != null) {
                    StatTile(
                        "Savings rate",
                        "%.0f%%".format(savingsRate),
                        Modifier.weight(1f),
                        if (savingsRate >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                } else {
                    StatTile("Top category", topCat, Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Bar chart
        item {
            val bars = buildChartBars(allExpenses, period, selectedTagId)
            if (bars.isNotEmpty()) {
                Text(
                    chartSectionTitle(period),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(4.dp))
                SpendingBarChart(bars = bars, modifier = Modifier.padding(horizontal = 8.dp))
                Spacer(Modifier.height(16.dp))
            }
        }

        // Donut chart
        if (categoryTotals.isNotEmpty()) {
            item {
                Text("By Category", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(4.dp))
                CategoryDonutChart(slices = categoryTotals, modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(16.dp))
            }
        }

        // Recent entries header
        item {
            Text("Recent Entries", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 16.dp))
        }

        if (expenses.isEmpty()) {
            item {
                Text(
                    "No expenses for this selection.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            items(expenses.take(20)) { item ->
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) {
                    ExpenseListItem(item)
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(horizontal = 4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurface else color,
            maxLines = 1
        )
    }
}

@Composable
fun SpendingBarChart(bars: List<ChartBar>, modifier: Modifier = Modifier) {
    val maxVal = bars.maxOf { maxOf(it.spent, it.income) }.coerceAtLeast(1.0)
    val hasIncome = bars.any { it.income > 0 }
    val spentColor = Color(0xFFC62828)
    val incomeColor = Color(0xFF2E7D32)
    val barWidthDp = 32.dp
    val chartWidth = (barWidthDp + 4.dp) * bars.size + 16.dp

    Box(modifier = modifier.height(220.dp)) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .width(chartWidth)
                .fillMaxHeight()
                .padding(start = 8.dp, end = 8.dp, bottom = 24.dp, top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            bars.forEach { bar ->
                val heightFraction = (bar.spent / maxVal).toFloat()
                Column(
                    modifier = Modifier.width(barWidthDp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Box(
                        modifier = Modifier
                            .width(barWidthDp)
                            .fillMaxHeight(heightFraction)
                            .padding(bottom = 0.dp)
                    ) {
                        Surface(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp),
                            color = spentColor.copy(alpha = 0.75f),
                            modifier = Modifier.fillMaxSize()
                        ) {}
                    }
                }
            }
        }
        // X-axis labels
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .horizontalScroll(rememberScrollState())
                .width(chartWidth)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val fmt = SimpleDateFormat("d", Locale.getDefault())
            bars.forEach { bar ->
                Text(
                    fmt.format(Date(bar.date)),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.width(barWidthDp),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun CategoryDonutChart(slices: List<CategorySlice>, modifier: Modifier = Modifier) {
    val colors = listOf(
        Color(0xFF1565C0), Color(0xFFC62828), Color(0xFF2E7D32), Color(0xFFE65100),
        Color(0xFF6A1B9A), Color(0xFF00838F), Color(0xFF558B2F), Color(0xFF4527A0)
    )
    val total = slices.sumOf { it.total }.coerceAtLeast(0.01)

    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(140.dp)) {
            var startAngle = -90f
            slices.forEachIndexed { i, slice ->
                val sweep = (slice.total / total * 360f).toFloat()
                drawArc(
                    color = colors[i % colors.size],
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = size.width * 0.22f, cap = StrokeCap.Butt),
                    topLeft = Offset(size.width * 0.11f, size.height * 0.11f),
                    size = Size(size.width * 0.78f, size.height * 0.78f)
                )
                startAngle += sweep
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            slices.take(6).forEachIndexed { i, slice ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .then(Modifier.padding(end = 0.dp))
                    ) {
                        Canvas(Modifier.fillMaxSize()) {
                            drawCircle(colors[i % colors.size])
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${slice.name}: ${formatCurrency(slice.total)}",
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private fun buildChartBars(
    allExpenses: List<ExpenseWithDetails>,
    period: TimePeriod,
    tagId: Long?
): List<ChartBar> {
    val cal = Calendar.getInstance()
    val now = cal.timeInMillis
    val intervals = mutableListOf<Pair<Long, Long>>()

    when (period) {
        TimePeriod.WEEK -> {
            val todayStart = cal.apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            for (i in 6 downTo 0) {
                val s = Calendar.getInstance().apply { timeInMillis = todayStart; add(Calendar.DAY_OF_YEAR, -i) }.timeInMillis
                val e = Calendar.getInstance().apply { timeInMillis = s; add(Calendar.DAY_OF_YEAR, 1) }.timeInMillis
                intervals.add(Pair(s, e))
            }
        }
        TimePeriod.MONTH -> {
            val monthStart = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val daysInMonth = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH)
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            for (d in 0 until daysInMonth) {
                val s = Calendar.getInstance().apply { timeInMillis = monthStart; add(Calendar.DAY_OF_YEAR, d) }.timeInMillis
                if (s > today) break
                val e = Calendar.getInstance().apply { timeInMillis = s; add(Calendar.DAY_OF_YEAR, 1) }.timeInMillis
                intervals.add(Pair(s, e))
            }
        }
        TimePeriod.YEAR -> {
            val yearStart = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val currentMonth = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            for (m in 0 until 12) {
                val s = Calendar.getInstance().apply { timeInMillis = yearStart; add(Calendar.MONTH, m) }.timeInMillis
                if (s > currentMonth) break
                val e = Calendar.getInstance().apply { timeInMillis = s; add(Calendar.MONTH, 1) }.timeInMillis
                intervals.add(Pair(s, e))
            }
        }
        TimePeriod.ALL -> {
            val earliest = allExpenses.minOfOrNull { it.expense.date } ?: return emptyList()
            val firstMonth = Calendar.getInstance().apply {
                timeInMillis = earliest; set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            var cursor = firstMonth
            while (cursor <= now) {
                val next = Calendar.getInstance().apply { timeInMillis = cursor; add(Calendar.MONTH, 1) }.timeInMillis
                intervals.add(Pair(cursor, next))
                cursor = next
            }
        }
    }

    return intervals.map { (start, end) ->
        val slice = allExpenses.filter { e ->
            e.expense.date >= start && e.expense.date < end &&
                (tagId == null || e.tags.any { t -> t.id == tagId })
        }
        ChartBar(
            date = start,
            spent = slice.filter { !it.expense.isIncome }.sumOf { it.expense.amount },
            income = slice.filter { it.expense.isIncome }.sumOf { it.expense.amount }
        )
    }
}

private fun chartSectionTitle(period: TimePeriod) = when (period) {
    TimePeriod.WEEK -> "Daily — Last 7 Days"
    TimePeriod.MONTH -> "Daily — This Month"
    TimePeriod.YEAR -> "Monthly — This Year"
    TimePeriod.ALL -> "Monthly — All Time"
}
