package com.example.smarthome

import android.app.DatePickerDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.smarthome.databinding.ActivityMonitoringListrikBinding
import com.example.smarthome.model.ElectricityHistoryModel
import com.example.smarthome.model.MonthlyElectricityHistoryModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.floor
import kotlin.math.max

class MonitoringListrikActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMonitoringListrikBinding
    private var currentActiveBar: View? = null
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val dayBarMap = mutableMapOf<Int, View>()
    private val dayDataMap = mutableMapOf<Int, ElectricityHistoryModel>()
    private val calendar = Calendar.getInstance()
    private val dataCache = mutableMapOf<String, Map<String, ElectricityHistoryModel>>()
    private var isLoading = false
    private val refreshHandler = Handler(Looper.getMainLooper())
    private var isAutoRefreshEnabled = true
    private val REFRESH_INTERVAL = 3000L

    init {
        try {
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build()
            firestore.firestoreSettings = settings
            Log.d("MonitoringListrik", "Firestore offline persistence enabled")
        } catch (e: Exception) {
            Log.e("MonitoringListrik", "Error enabling offline persistence: ${e.message}")
        }
    }

    private val refreshRunnable = object : Runnable {
        override fun run() {
            if (isAutoRefreshEnabled && !isLoading) {
                Log.d("MonitoringListrik", "Auto-refresh: updating data...")
                val currentDate = calendar.clone() as Calendar
                loadElectricityDataForWeek(currentDate, forceRefresh = true)
                loadMonthlyCostData()
            }
            if (isAutoRefreshEnabled) {
                refreshHandler.postDelayed(this, REFRESH_INTERVAL)
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMonitoringListrikBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.btnBack.setOnClickListener {
            finish()
        }
        setupMonthYearSpinner()
        setupChartBars()
        signInAnonymously()
    }

    override fun onResume() {
        super.onResume()
        isAutoRefreshEnabled = true
        startAutoRefresh()
        Log.d("MonitoringListrik", "Auto-refresh started (every 3 seconds)")
    }

    override fun onPause() {
        super.onPause()
        // Stop auto-refresh when activity is not visible
        stopAutoRefresh()
        Log.d("MonitoringListrik", "Auto-refresh stopped")
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAutoRefresh()
    }

    private fun startAutoRefresh() {
        refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL)
    }

    private fun stopAutoRefresh() {
        isAutoRefreshEnabled = false
        refreshHandler.removeCallbacks(refreshRunnable)
    }

    private fun signInAnonymously() {
        if (auth.currentUser != null) {
            Log.d("MonitoringListrik", "User already signed in: ${auth.currentUser?.uid}")
            loadInitialData()
            return
        }
        auth.signInAnonymously()
            .addOnSuccessListener {
                Log.d("MonitoringListrik", "Anonymous sign in successful: ${it.user?.uid}")
                loadInitialData()
            }
            .addOnFailureListener { exception ->
                Log.e("MonitoringListrik", "Anonymous sign in failed", exception)
                Log.w("MonitoringListrik", "Continuing without auth due to public Firestore rules")
                loadInitialData()
            }
    }
    private fun loadInitialData() {
        loadElectricityDataForWeek(calendar)
        loadMonthlyCostData()
    }
    private fun setupMonthYearSpinner() {
        val months = arrayOf(
            "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
        )
        val anchor = binding.spinnerMonthYear
        val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale("id", "ID"))
        val defaultSelection = dateFormat.format(calendar.time)
        anchor.text = defaultSelection
        anchor.setOnClickListener {
            val pickerCalendar = Calendar.getInstance()
            val currentText = anchor.text.toString()
            val parts = currentText.split(" ")
            if (parts.size == 3) {
                val monthIndex = months.indexOf(parts[1])
                val year = parts[2].toIntOrNull()
                if (monthIndex >= 0 && year != null) {
                    pickerCalendar.set(Calendar.YEAR, year)
                    pickerCalendar.set(Calendar.MONTH, monthIndex)
                }
            }
            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    val selectedCalendar = Calendar.getInstance()
                    selectedCalendar.set(year, month, dayOfMonth)
                    val selected = dateFormat.format(selectedCalendar.time)
                    anchor.text = selected
                    loadElectricityDataForWeek(selectedCalendar)
                },
                pickerCalendar.get(Calendar.YEAR),
                pickerCalendar.get(Calendar.MONTH),
                pickerCalendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.datePicker.minDate = Calendar.getInstance().apply {
                set(Calendar.YEAR, 2020)
                set(Calendar.MONTH, 0)
                set(Calendar.DAY_OF_MONTH, 1)
            }.timeInMillis
            datePickerDialog.datePicker.maxDate = Calendar.getInstance().timeInMillis
            datePickerDialog.show()
        }
    }

    private fun updateChartData(month: String, year: Int) {
        Log.d("MonitoringListrik", "Update chart for $month $year")
        // TODO: Implementasi update chart berdasarkan bulan dan tahun
    }

    private fun setupChartBars() {
        dayBarMap[1] = binding.barSen
        dayBarMap[2] = binding.barSel
        dayBarMap[3] = binding.barRab
        dayBarMap[4] = binding.barKam
        dayBarMap[5] = binding.barJum
        dayBarMap[6] = binding.barSab
        dayBarMap[7] = binding.barMing
        val todayDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val adjustedToday = if (todayDayOfWeek == 1) 7 else todayDayOfWeek - 1
        currentActiveBar = dayBarMap[adjustedToday]
        binding.chartFrameContainer.post {
            currentActiveBar?.let { bar ->
                bar.setBackgroundResource(R.drawable.listrik_history_blue_bar_background)
                positionLabelAboveBar(bar, distanceDp = 10f)
                updateMovedLabelValue(bar)
            }
        }
        binding.barContainerSen.setOnClickListener { onBarClicked(binding.barSen) }
        binding.barContainerSel.setOnClickListener { onBarClicked(binding.barSel) }
        binding.barContainerRab.setOnClickListener { onBarClicked(binding.barRab) }
        binding.barContainerKam.setOnClickListener { onBarClicked(binding.barKam) }
        binding.barContainerJum.setOnClickListener { onBarClicked(binding.barJum) }
        binding.barContainerSab.setOnClickListener { onBarClicked(binding.barSab) }
        binding.barContainerMing.setOnClickListener { onBarClicked(binding.barMing) }
    }

    /**
     * Load data dari Firestore untuk 1 minggu dari tanggal yang dipilih
     */
    private fun loadElectricityDataForWeek(selectedDate: Calendar, forceRefresh: Boolean = false) {
        if (isLoading) {
            Log.d("MonitoringListrik", "Already loading data, skipping...")
            return
        }
        Log.d("MonitoringListrik", "Loading data for week around: ${selectedDate.time}")
        isLoading = true
        if (!forceRefresh) {
            binding.spinnerMonthYear.isEnabled = false
            binding.loadingContainer.visibility = View.VISIBLE
        }
        dayDataMap.clear()
        val endDate = selectedDate.clone() as Calendar
        val startDate = selectedDate.clone() as Calendar
        startDate.add(Calendar.DAY_OF_MONTH, -6)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val startDateStr = dateFormat.format(startDate.time)
        val endDateStr = dateFormat.format(endDate.time)
        val cacheKey = "$startDateStr-$endDateStr"
        Log.d("MonitoringListrik", "Date range: $startDateStr to $endDateStr")
        if (!forceRefresh && dataCache.containsKey(cacheKey)) {
            Log.d("MonitoringListrik", "Using cached data")
            val weekDataMap = dataCache[cacheKey]!!
            processWeekData(startDate, weekDataMap)
            isLoading = false
            binding.spinnerMonthYear.isEnabled = true
            binding.loadingContainer.visibility = View.GONE
            return
        }
        firestore.collection("electricity_history")
            .whereGreaterThanOrEqualTo("date", startDateStr)
            .whereLessThanOrEqualTo("date", endDateStr)
            .get()
            .addOnSuccessListener { documents ->
                Log.d("MonitoringListrik", "Found ${documents.size()} documents (forceRefresh: $forceRefresh)")
                val weekDataMap = mutableMapOf<String, ElectricityHistoryModel>()
                for (document in documents) {
                    try {
                        val data = document.toObject(ElectricityHistoryModel::class.java)
                        weekDataMap[data.date] = data
                        Log.d("MonitoringListrik", "Date: ${data.date}, kWh: ${data.totalDaya_kWh}, Wh: ${data.totalDaya_Wh}")
                    } catch (e: Exception) {
                        Log.e("MonitoringListrik", "Error parsing document: ${e.message}")
                    }
                }
                dataCache[cacheKey] = weekDataMap
                processWeekData(startDate, weekDataMap)
                isLoading = false
                binding.spinnerMonthYear.isEnabled = true
                binding.loadingContainer.visibility = View.GONE
            }
            .addOnFailureListener { exception ->
                Log.e("MonitoringListrik", "Error getting documents: ", exception)
                if (!forceRefresh) {
                    Toast.makeText(this, "Gagal memuat data: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
                for (day in 1..7) {
                    dayDataMap[day] = ElectricityHistoryModel()
                }
                updateChartWithData()
                isLoading = false
                binding.spinnerMonthYear.isEnabled = true
                binding.loadingContainer.visibility = View.GONE
            }
    }

    /**
     * Process week data and map to day bars
     */
    private fun processWeekData(startDate: Calendar, weekDataMap: Map<String, ElectricityHistoryModel>) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val tempCalendar = startDate.clone() as Calendar
        for (i in 0..6) {
            val dateStr = dateFormat.format(tempCalendar.time)
            val dayOfWeek = tempCalendar.get(Calendar.DAY_OF_WEEK)
            val adjustedDay = if (dayOfWeek == 1) 7 else dayOfWeek - 1
            dayDataMap[adjustedDay] = weekDataMap[dateStr]
                ?: ElectricityHistoryModel(date = dateStr)
            Log.d("MonitoringListrik", "Mapped date $dateStr to day: $adjustedDay")
            tempCalendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        updateChartWithData()
    }
    private fun loadElectricityData(month: Int, year: Int) {
        Log.d("MonitoringListrik", "Loading data for month: $month, year: $year")
    }

    /**
     * Update chart dengan data yang sudah dimuat
     */
    private fun updateChartWithData() {
        var maxValue = 0.0
        dayDataMap.values.forEach { data ->
            val value = if (data.totalDaya_kWh >= 1.0) data.totalDaya_kWh else data.totalDaya_Wh / 1000.0
            if (value > maxValue) {
                maxValue = value
            }
        }
        Log.d("MonitoringListrik", "Max value: $maxValue")
        val density = resources.displayMetrics.density
        val maxHeightDp = 240f
        val minHeightDp = 20f
        for (day in 1..7) {
            val bar = dayBarMap[day]
            if (bar != null) {
                val data = dayDataMap[day]
                if (data != null && (data.totalDaya_kWh > 0 || data.totalDaya_Wh > 0)) {
                    val value = if (data.totalDaya_kWh >= 1.0) data.totalDaya_kWh else data.totalDaya_Wh / 1000.0
                    val heightDp = if (maxValue > 0) {
                        val ratio = (value / maxValue).toFloat()
                        max(minHeightDp, minHeightDp + (maxHeightDp - minHeightDp) * ratio)
                    } else {
                        minHeightDp
                    }
                    val heightPx = (heightDp * density).toInt()
                    val layoutParams = bar.layoutParams
                    layoutParams.height = heightPx
                    bar.layoutParams = layoutParams
                    bar.tag = data
                    Log.d("MonitoringListrik", "Day $day: value=$value, heightDp=$heightDp")
                } else {
                    val heightPx = (minHeightDp * density).toInt()
                    val layoutParams = bar.layoutParams
                    layoutParams.height = heightPx
                    bar.layoutParams = layoutParams
                    bar.tag = ElectricityHistoryModel()
                    Log.d("MonitoringListrik", "Day $day: no data or zero")
                }
            }
        }
        binding.chartFrameContainer.post {
            currentActiveBar?.let { bar ->
                positionLabelAboveBar(bar, distanceDp = 10f)
                updateMovedLabelValue(bar)
            }
        }
    }

    /**
     * Called when a bar is clicked. We'll position the floating label above
     * the clicked bar and update its value text. Also set bar colors.
     */
    private fun onBarClicked(clickedBar: View) {
        val bars = listOf(
            binding.barSen,
            binding.barSel,
            binding.barRab,
            binding.barKam,
            binding.barJum,
            binding.barSab,
            binding.barMing
        )
        bars.forEach { bar ->
            bar.setBackgroundResource(R.drawable.listrik_history_gray_bar_background)
        }
        clickedBar.setBackgroundResource(R.drawable.listrik_history_blue_bar_background)
        currentActiveBar = clickedBar
        positionLabelAboveBar(clickedBar, distanceDp = 10f)
        updateMovedLabelValue(clickedBar)
        Log.d("ChartBar", "Bar clicked")
    }
    private fun positionLabelAboveBar(bar: View, distanceDp: Float = 10f) {
        val label = binding.valueLabelKam
        bar.post {
            if (label.width == 0 || label.height == 0) {
                label.measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                )
            }
            val barContainer = bar.parent as View
            val barContainerLocation = IntArray(2)
            barContainer.getLocationInWindow(barContainerLocation)
            val frameLocation = IntArray(2)
            binding.chartFrameContainer.getLocationInWindow(frameLocation)
            val relativeX = barContainerLocation[0] - frameLocation[0]
            val relativeY = barContainerLocation[1] - frameLocation[1]
            val labelWidth = if (label.width > 0) label.width else label.measuredWidth
            val labelHeight = if (label.height > 0) label.height else label.measuredHeight
            val barContainerWidth = barContainer.width
            val labelX = relativeX + (barContainerWidth - labelWidth) / 2
            val density = resources.displayMetrics.density
            val marginAboveBar = (distanceDp * density).toInt()
            val barTop = relativeY + (barContainer.height - bar.height)
            val labelY = barTop - labelHeight - marginAboveBar
            val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            params.leftMargin = labelX
            params.topMargin = labelY
            label.layoutParams = params
            label.visibility = View.VISIBLE
            label.bringToFront()
            label.invalidate()
            Log.d("MonitoringListrik", "Label positioned at: X=$labelX, Y=$labelY, containerWidth=$barContainerWidth, labelWidth=$labelWidth")
        }
    }

    private fun updateMovedLabelValue(bar: View) {
        val data = bar.tag as? ElectricityHistoryModel
        val display = if (data != null && (data.totalDaya_kWh > 0 || data.totalDaya_Wh > 0)) {
            if (data.totalDaya_kWh >= 1.0) {
                val flooredValue = floor(data.totalDaya_kWh * 100) / 100
                String.format(Locale.getDefault(), "%.2f kWh", flooredValue)
            } else {
                val flooredValue = floor(data.totalDaya_Wh * 100) / 100
                String.format(Locale.getDefault(), "%.2f Wh", flooredValue)
            }
        } else {
            "0 Wh"
        }
        binding.valueLabelTextKam.text = display
        Log.d("MonitoringListrik", "Label updated: $display")
    }

    /**
     * Load data total biaya bulanan dari Firestore
     */
    private fun loadMonthlyCostData() {
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) + 1
        val documentId = String.format("%d-%02d", currentYear, currentMonth)
        Log.d("MonitoringListrik", "Loading monthly cost for: $documentId")
        firestore.collection("monthly_electricity_history")
            .document(documentId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    try {
                        val monthlyData = document.toObject(MonthlyElectricityHistoryModel::class.java)
                        if (monthlyData != null) {
                            val totalBiaya = monthlyData.totalBiaya_Rp
                            val formattedBiaya = formatCurrency(totalBiaya)
                            binding.savedCostText.text = formattedBiaya
                            Log.d("MonitoringListrik", "Monthly cost loaded: Rp $totalBiaya -> $formattedBiaya")
                        } else {
                            binding.savedCostText.text = "Rp 0"
                            Log.w("MonitoringListrik", "Monthly data is null")
                        }
                    } catch (e: Exception) {
                        Log.e("MonitoringListrik", "Error parsing monthly data: ${e.message}")
                        binding.savedCostText.text = "Rp 0"
                    }
                } else {
                    binding.savedCostText.text = "Rp 0"
                    Log.d("MonitoringListrik", "No monthly data found for $documentId")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("MonitoringListrik", "Error getting monthly cost: ", exception)
                binding.savedCostText.text = "Rp 0"
            }
    }

    private fun formatCurrency(amount: Double): String {
        val numberFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        return if (amount % 1.0 == 0.0) {
            "Rp ${String.format("%,.0f", amount)}"
        } else {
            numberFormat.format(amount).replace("Rp", "Rp ")
        }
    }
}
