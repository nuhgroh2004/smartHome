package com.example.smarthome

import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListPopupWindow
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.smarthome.databinding.ActivityMonitoringListrikBinding
import java.util.Calendar

class MonitoringListrikActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMonitoringListrikBinding

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
}