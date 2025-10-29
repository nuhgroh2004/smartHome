package com.example.smarthome

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.ListPopupWindow
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.smarthome.databinding.ActivityMonitoringListrikBinding
import java.util.Calendar
import java.util.Locale

class MonitoringListrikActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMonitoringListrikBinding
    private var currentActiveBar: View? = null

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
    }

    private fun setupMonthYearSpinner() {
        val months = arrayOf(
            "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
        )

        // Dapatkan tanggal saat ini
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)

        val anchor = binding.spinnerMonthYear

        // Set default ke bulan dan tahun saat ini
        val defaultSelection = "${months[currentMonth]} $currentYear"
        anchor.text = defaultSelection

        // Tampilkan DatePicker ketika anchor diklik
        anchor.setOnClickListener {
            // Buat calendar untuk DatePicker dengan nilai saat ini yang dipilih
            val pickerCalendar = Calendar.getInstance()

            // Parse text yang sedang ditampilkan untuk mendapatkan bulan dan tahun
            val currentText = anchor.text.toString()
            val parts = currentText.split(" ")
            if (parts.size == 2) {
                val monthIndex = months.indexOf(parts[0])
                val year = parts[1].toIntOrNull()
                if (monthIndex >= 0 && year != null) {
                    pickerCalendar.set(Calendar.YEAR, year)
                    pickerCalendar.set(Calendar.MONTH, monthIndex)
                }
            }

            // Buat DatePickerDialog
            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, _ ->
                    // Handler ketika tanggal dipilih
                    val selectedMonth = months[month]
                    val selected = "$selectedMonth $year"
                    anchor.text = selected

                    // Update chart dengan data baru
                    updateChartData(selectedMonth, year)
                },
                pickerCalendar.get(Calendar.YEAR),
                pickerCalendar.get(Calendar.MONTH),
                1 // Hari tidak terlalu penting, set ke 1
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
        // Set bar Kam sebagai default aktif
        currentActiveBar = binding.barKam

        // Wait for layout to be measured before positioning label
        binding.chartFrameContainer.post {
            // Position label above bar Sen by default, 10dp distance
            positionLabelAboveBar(binding.barKam, distanceDp = 50f)
            updateMovedLabelValue(binding.barKam)
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
        positionLabelAboveBar(clickedBar, distanceDp = 50f)

        // Update value from tag but treat 20dp (or <=20dp) as 0.0 kWh
        updateMovedLabelValue(clickedBar)

        Log.d("ChartBar", "Bar clicked with (raw tag) value: ${clickedBar.tag}")
    }

    // Position the floating label above the specified bar using absolute coordinates in root FrameLayout
    private fun positionLabelAboveBar(bar: View, distanceDp: Float = 50f) {
        val label = binding.valueLabelKam

        // Ensure label is measured first if not yet measured
        if (label.width == 0 || label.height == 0) {
            label.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
        }

        // Get bar position in window coordinates
        val barLocation = IntArray(2)
        bar.getLocationInWindow(barLocation)

        // Get root main container position in window coordinates
        val mainLocation = IntArray(2)
        findViewById<View>(R.id.main).getLocationInWindow(mainLocation)

        // Calculate relative position to root FrameLayout
        val relativeX = barLocation[0] - mainLocation[0]
        val relativeY = barLocation[1] - mainLocation[1]

        // Center the label horizontally above the bar
        val labelWidth = if (label.width > 0) label.width else label.measuredWidth
        val barWidth = bar.width
        val labelX = relativeX + (barWidth - labelWidth) / 2

        // Position label above the bar with specified distance in dp
        val density = resources.displayMetrics.density
        val marginAboveBar = (distanceDp * density).toInt()

        // Get actual label height (measured or current)
        val labelHeight = if (label.height > 0) label.height else label.measuredHeight

        // Calculate Y position: bar top - label height - margin
        val labelY = relativeY - labelHeight - marginAboveBar

        // Update FrameLayout params with absolute position (relative to root)
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
    }

    // Compute and set the text on the moved label based on clicked bar's height or tag
    private fun updateMovedLabelValue(bar: View) {
        val density = resources.displayMetrics.density
        val heightPx = bar.height

        val valueFloat: Float = if (heightPx > 0) {
            val heightDp = if (density > 0) heightPx / density else 0f
            // If bar is at or below 20dp consider it 0.0 kWh
            if (heightDp <= 20f + 0.1f) {
                0.0f
            } else {
                val tagStr = bar.tag as? String
                tagStr?.toFloatOrNull() ?: 0.0f
            }
        } else {
            // Height not measured yet (e.g., called before layout). Fall back to the tag value.
            val tagStr = bar.tag as? String
            tagStr?.toFloatOrNull() ?: 0.0f
        }

        val display = String.format(Locale.getDefault(), "%.1f kWh", valueFloat)

        // Update the TextView inside the moved label (id: value_label_text_kam)
        binding.valueLabelTextKam.text = display
    }
}