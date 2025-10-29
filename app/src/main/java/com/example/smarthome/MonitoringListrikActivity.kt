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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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

    // Cache untuk menyimpan data yang sudah dimuat
    private val dataCache = mutableMapOf<String, Map<String, ElectricityHistoryModel>>()
    private var isLoading = false

    // Handler untuk auto-refresh setiap 3 detik
    private val refreshHandler = Handler(Looper.getMainLooper())
    private var isAutoRefreshEnabled = true
    private val REFRESH_INTERVAL = 3000L // 3 detik

    private val refreshRunnable = object : Runnable {
        override fun run() {
            if (isAutoRefreshEnabled && !isLoading) {
                Log.d("MonitoringListrik", "Auto-refresh: updating data...")
                // Clear cache untuk force refresh dari Firestore
                val currentDate = calendar.clone() as Calendar
                loadElectricityDataForWeek(currentDate, forceRefresh = true)
            }
            // Schedule next refresh
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

        // Sign in anonymously untuk mendapatkan permission
        signInAnonymously()
    }

    override fun onResume() {
        super.onResume()
        // Start auto-refresh when activity is visible
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
        // Cek apakah sudah login
        if (auth.currentUser != null) {
            Log.d("MonitoringListrik", "User already signed in: ${auth.currentUser?.uid}")
            loadInitialData()
            return
        }

        // Sign in secara anonymous
        auth.signInAnonymously()
            .addOnSuccessListener {
                Log.d("MonitoringListrik", "Anonymous sign in successful: ${it.user?.uid}")
                loadInitialData()
            }
            .addOnFailureListener { exception ->
                Log.e("MonitoringListrik", "Anonymous sign in failed", exception)

                // Tetap lanjutkan load data meskipun sign in gagal
                // Firestore dengan rules allow read/write: if true tetap bisa diakses
                Log.w("MonitoringListrik", "Continuing without auth due to public Firestore rules")
                loadInitialData()
            }
    }

    private fun loadInitialData() {
        // Load data untuk minggu ini (dari hari ini)
        loadElectricityDataForWeek(calendar)
    }

    private fun setupMonthYearSpinner() {
        val months = arrayOf(
            "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
        )

        val anchor = binding.spinnerMonthYear

        // Set default ke hari ini (tanggal lengkap)
        val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale("id", "ID"))
        val defaultSelection = dateFormat.format(calendar.time)
        anchor.text = defaultSelection

        // Tampilkan DatePicker ketika anchor diklik
        anchor.setOnClickListener {
            // Buat calendar untuk DatePicker dengan nilai saat ini yang dipilih
            val pickerCalendar = Calendar.getInstance()

            // Parse text yang sedang ditampilkan untuk mendapatkan bulan dan tahun
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

            // Buat DatePickerDialog
            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    // Handler ketika tanggal dipilih
                    val selectedCalendar = Calendar.getInstance()
                    selectedCalendar.set(year, month, dayOfMonth)
                    val selected = dateFormat.format(selectedCalendar.time)
                    anchor.text = selected

                    // Update chart dengan data minggu dari tanggal yang dipilih
                    loadElectricityDataForWeek(selectedCalendar)
                },
                pickerCalendar.get(Calendar.YEAR),
                pickerCalendar.get(Calendar.MONTH),
                pickerCalendar.get(Calendar.DAY_OF_MONTH)
            )

            // Atur range tahun yang bisa dipilih (opsional)
            datePickerDialog.datePicker.minDate = Calendar.getInstance().apply {
                set(Calendar.YEAR, 2020)
                set(Calendar.MONTH, 0)
                set(Calendar.DAY_OF_MONTH, 1)
            }.timeInMillis

            datePickerDialog.datePicker.maxDate = Calendar.getInstance().timeInMillis

            // Tampilkan dialog
            datePickerDialog.show()
        }
    }

    private fun updateChartData(month: String, year: Int) {
        // Menandai parameter digunakan agar tidak muncul peringatan UNUSED_PARAMETER
        Log.d("MonitoringListrik", "Update chart for $month $year")
        // TODO: Implementasi update chart berdasarkan bulan dan tahun
    }

    private fun setupChartBars() {
        // Map bar dengan hari dalam minggu (1=Senin, 7=Minggu)
        dayBarMap[1] = binding.barSen
        dayBarMap[2] = binding.barSel
        dayBarMap[3] = binding.barRab
        dayBarMap[4] = binding.barKam
        dayBarMap[5] = binding.barJum
        dayBarMap[6] = binding.barSab
        dayBarMap[7] = binding.barMing

        // Set bar hari ini sebagai default aktif
        val todayDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val adjustedToday = if (todayDayOfWeek == 1) 7 else todayDayOfWeek - 1
        currentActiveBar = dayBarMap[adjustedToday]

        // Wait for layout to be measured before positioning label
        binding.chartFrameContainer.post {
            // Position label above bar hari ini by default, 10dp distance
            currentActiveBar?.let { bar ->
                bar.setBackgroundResource(R.drawable.listrik_history_blue_bar_background)
                positionLabelAboveBar(bar, distanceDp = 10f)
                updateMovedLabelValue(bar)
            }
        }

        // Setup click listener untuk setiap bar container
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
        // Prevent multiple simultaneous loads
        if (isLoading) {
            Log.d("MonitoringListrik", "Already loading data, skipping...")
            return
        }

        Log.d("MonitoringListrik", "Loading data for week around: ${selectedDate.time}")

        // Show loading indicator only for manual refresh
        isLoading = true
        if (!forceRefresh) {
            binding.spinnerMonthYear.isEnabled = false
            binding.loadingContainer.visibility = View.VISIBLE
        }

        // Clear data sebelumnya
        dayDataMap.clear()

        // Hitung range 1 minggu (7 hari sebelum tanggal yang dipilih sampai tanggal yang dipilih)
        val endDate = selectedDate.clone() as Calendar
        val startDate = selectedDate.clone() as Calendar
        startDate.add(Calendar.DAY_OF_MONTH, -6) // 6 hari sebelumnya + hari ini = 7 hari

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val startDateStr = dateFormat.format(startDate.time)
        val endDateStr = dateFormat.format(endDate.time)

        // Create cache key
        val cacheKey = "$startDateStr-$endDateStr"

        Log.d("MonitoringListrik", "Date range: $startDateStr to $endDateStr")

        // Check cache first (skip if force refresh)
        if (!forceRefresh && dataCache.containsKey(cacheKey)) {
            Log.d("MonitoringListrik", "Using cached data")
            val weekDataMap = dataCache[cacheKey]!!
            processWeekData(startDate, weekDataMap)
            isLoading = false
            binding.spinnerMonthYear.isEnabled = true
            binding.loadingContainer.visibility = View.GONE
            return
        }

        // Query Firestore untuk mendapatkan data dalam range 1 minggu
        firestore.collection("electricity_history")
            .whereGreaterThanOrEqualTo("date", startDateStr)
            .whereLessThanOrEqualTo("date", endDateStr)
            .get()
            .addOnSuccessListener { documents ->
                Log.d("MonitoringListrik", "Found ${documents.size()} documents (forceRefresh: $forceRefresh)")

                // Map untuk menyimpan data berdasarkan tanggal
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

                // Save to cache
                dataCache[cacheKey] = weekDataMap

                processWeekData(startDate, weekDataMap)

                // Hide loading indicator
                isLoading = false
                binding.spinnerMonthYear.isEnabled = true
                binding.loadingContainer.visibility = View.GONE
            }
            .addOnFailureListener { exception ->
                Log.e("MonitoringListrik", "Error getting documents: ", exception)
                if (!forceRefresh) {
                    Toast.makeText(this, "Gagal memuat data: ${exception.message}", Toast.LENGTH_SHORT).show()
                }

                // Tampilkan data kosong (semua 0)
                for (day in 1..7) {
                    dayDataMap[day] = ElectricityHistoryModel()
                }
                updateChartWithData()

                // Hide loading indicator
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

        // Map data ke hari dalam minggu (1=Senin, ..., 7=Minggu)
        val tempCalendar = startDate.clone() as Calendar
        for (i in 0..6) {
            val dateStr = dateFormat.format(tempCalendar.time)
            val dayOfWeek = tempCalendar.get(Calendar.DAY_OF_WEEK)
            val adjustedDay = if (dayOfWeek == 1) 7 else dayOfWeek - 1

            // Ambil data dari map, jika tidak ada maka null
            dayDataMap[adjustedDay] = weekDataMap[dateStr]
                ?: ElectricityHistoryModel(date = dateStr) // Data kosong dengan nilai 0

            Log.d("MonitoringListrik", "Mapped date $dateStr to day: $adjustedDay")
            tempCalendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        // Update chart setelah data dimuat
        updateChartWithData()
    }

    private fun loadElectricityData(month: Int, year: Int) {
        // Method lama, bisa dihapus atau digunakan untuk keperluan lain
        Log.d("MonitoringListrik", "Loading data for month: $month, year: $year")
    }

    /**
     * Update chart dengan data yang sudah dimuat
     */
    private fun updateChartWithData() {
        // Cari nilai maksimum untuk scaling
        var maxValue = 0.0
        dayDataMap.values.forEach { data ->
            val value = if (data.totalDaya_kWh >= 1.0) data.totalDaya_kWh else data.totalDaya_Wh / 1000.0
            if (value > maxValue) {
                maxValue = value
            }
        }

        Log.d("MonitoringListrik", "Max value: $maxValue")

        val density = resources.displayMetrics.density
        val maxHeightDp = 240f // Tinggi maksimal bar
        val minHeightDp = 20f  // Tinggi minimal bar (untuk data 0 atau null)

        // Update setiap bar
        for (day in 1..7) {
            val bar = dayBarMap[day]
            if (bar != null) {
                val data = dayDataMap[day]

                if (data != null && (data.totalDaya_kWh > 0 || data.totalDaya_Wh > 0)) {
                    // Hitung tinggi bar berdasarkan nilai
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

                    // Simpan nilai di tag untuk digunakan saat klik
                    bar.tag = data

                    Log.d("MonitoringListrik", "Day $day: value=$value, heightDp=$heightDp")
                } else {
                    // Tidak ada data atau data = 0, set ke minimal
                    val heightPx = (minHeightDp * density).toInt()
                    val layoutParams = bar.layoutParams
                    layoutParams.height = heightPx
                    bar.layoutParams = layoutParams
                    // Simpan data kosong untuk menampilkan "0 Wh"
                    bar.tag = ElectricityHistoryModel()

                    Log.d("MonitoringListrik", "Day $day: no data or zero")
                }
            }
        }

        // Update label untuk bar yang sedang aktif
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
        // Reset semua bar ke warna gray
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

        // Set bar yang diklik menjadi biru
        clickedBar.setBackgroundResource(R.drawable.listrik_history_blue_bar_background)
        currentActiveBar = clickedBar

        // Position the floating label above the clicked bar with 10dp distance
        positionLabelAboveBar(clickedBar, distanceDp = 10f)

        // Update value from tag
        updateMovedLabelValue(clickedBar)

        Log.d("ChartBar", "Bar clicked")
    }

    // Position the floating label above the specified bar using absolute coordinates in root FrameLayout
    private fun positionLabelAboveBar(bar: View, distanceDp: Float = 10f) {
        val label = binding.valueLabelKam

        // Pastikan layout sudah selesai
        bar.post {
            // Ensure label is measured first if not yet measured
            if (label.width == 0 || label.height == 0) {
                label.measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                )
            }

            // Get bar's parent (bar_container) position
            val barContainer = bar.parent as View

            // Get positions relative to the frame container
            val barContainerLocation = IntArray(2)
            barContainer.getLocationInWindow(barContainerLocation)

            val frameLocation = IntArray(2)
            binding.chartFrameContainer.getLocationInWindow(frameLocation)

            // Calculate relative position to FrameLayout
            val relativeX = barContainerLocation[0] - frameLocation[0]
            val relativeY = barContainerLocation[1] - frameLocation[1]

            // Get label dimensions
            val labelWidth = if (label.width > 0) label.width else label.measuredWidth
            val labelHeight = if (label.height > 0) label.height else label.measuredHeight

            // Get bar container width for centering
            val barContainerWidth = barContainer.width

            // Center the label horizontally above the bar container (not just the bar)
            val labelX = relativeX + (barContainerWidth - labelWidth) / 2

            // Position label above the bar with specified distance in dp
            val density = resources.displayMetrics.density
            val marginAboveBar = (distanceDp * density).toInt()

            // Get bar position within its container
            val barTop = relativeY + (barContainer.height - bar.height)

            // Calculate Y position: bar top - label height - margin
            val labelY = barTop - labelHeight - marginAboveBar

            // Update FrameLayout params with absolute position
            val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            params.leftMargin = labelX
            params.topMargin = labelY

            label.layoutParams = params
            label.visibility = View.VISIBLE

            // Force label to be drawn on top
            label.bringToFront()
            label.invalidate()

            Log.d("MonitoringListrik", "Label positioned at: X=$labelX, Y=$labelY, containerWidth=$barContainerWidth, labelWidth=$labelWidth")
        }
    }

    // Compute and set the text on the moved label based on clicked bar's data
    private fun updateMovedLabelValue(bar: View) {
        val data = bar.tag as? ElectricityHistoryModel

        val display = if (data != null && (data.totalDaya_kWh > 0 || data.totalDaya_Wh > 0)) {
            // Jika kWh >= 1, tampilkan dalam kWh
            if (data.totalDaya_kWh >= 1.0) {
                // Bulatkan ke bawah (floor) untuk 2 desimal
                val flooredValue = floor(data.totalDaya_kWh * 100) / 100
                String.format(Locale.getDefault(), "%.2f kWh", flooredValue)
            } else {
                // Jika kWh < 1, tampilkan dalam Wh
                // Bulatkan ke bawah (floor) untuk 2 desimal
                val flooredValue = floor(data.totalDaya_Wh * 100) / 100
                String.format(Locale.getDefault(), "%.2f Wh", flooredValue)
            }
        } else {
            // Tidak ada data atau data = 0
            "0 Wh"
        }

        // Update the TextView inside the moved label (id: value_label_text_kam)
        binding.valueLabelTextKam.text = display

        Log.d("MonitoringListrik", "Label updated: $display")
    }
}
