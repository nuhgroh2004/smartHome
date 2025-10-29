package com.example.smarthome.model

import com.google.firebase.Timestamp

data class MonthlyElectricityHistoryModel(
    val jumlahHari: Int = 0,
    val lastUpdatedAt: Timestamp? = null,
    val month: Int = 0,
    val rataRataHarian_kWh: Double = 0.0,
    val totalBiaya_Rp: Double = 0.0,
    val totalDaya_Wh: Double = 0.0,
    val totalKonsumsi_kWh: Double = 0.0,
    val year: Int = 0
)

