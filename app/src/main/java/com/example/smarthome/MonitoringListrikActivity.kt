package com.example.smarthome

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

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val years = (2025..currentYear).toList()

        // Generate list bulan-tahun
        val monthYearList = mutableListOf<String>()
        for (year in years.reversed()) {
            for (month in months) {
                monthYearList.add("$month $year")
            }
        }

        // Setup adapter (digunakan oleh ListPopupWindow)
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            monthYearList
        )

        // Anchor adalah TextView dengan id spinner_month_year (diganti di layout)
        val anchor = binding.spinnerMonthYear

        // Hitung ukuran dalam px - kurangi tinggi agar hanya 3 item terlihat
        val density = resources.displayMetrics.density
        val heightPx = (120 * density + 0.5f).toInt() // Dikurangi dari 144dp ke 120dp untuk memastikan hanya ~3 item
        val minWidthPx = (180 * density + 0.5f).toInt()

        // Buat ListPopupWindow agar kita bisa mengontrol tinggi popup
        val listPopup = ListPopupWindow(this)
        listPopup.anchorView = anchor
        listPopup.setAdapter(adapter)
        listPopup.height = heightPx
        listPopup.width = minWidthPx
        listPopup.isModal = true

        // Set default ke bulan dan tahun saat ini pada anchor (TextView)
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        val defaultSelection = "${months[currentMonth]} $currentYear"
        anchor.text = defaultSelection

        // Handler ketika item dipilih di popup
        listPopup.setOnItemClickListener { _, _, position, _ ->
            val selected = monthYearList[position]
            anchor.text = selected

            val parts = selected.split(" ")
            val month = parts[0]
            val year = parts[1].toInt()

            // Tutup popup lalu update chart
            listPopup.dismiss()
            updateChartData(month, year)
        }

        // Tampilkan popup ketika anchor diklik
        anchor.setOnClickListener {
            // Jika lebar anchor belum di-measure, atur width popup minimal
            if (anchor.width > 0) {
                listPopup.width = anchor.width
            }
            listPopup.show()
        }

        // Catatan: jumlah item yang terlihat dibatasi menjadi 3 dan dapat discroll via height di ListPopupWindow (120dp)
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