package com.example.smarthome.model

import com.google.firebase.Timestamp

data class ElectricityHistoryModel(
    val biayaHarian_Rp: Double = 0.0,
    val date: String = "",
    val day: Int = 0,
    val dayaTerakhir_W: Int = 0,
    val firstRecordedAt: Timestamp? = null,
    val jumlahPembacaan: Int = 0,
    val lastPembacaanAt: Timestamp? = null,
    val lastUpdatedAt: Timestamp? = null,
    val month: Int = 0,
    val rataRata_W: Double = 0.0,
    val totalDaya_Wh: Double = 0.0,
    val totalDaya_kWh: Double = 0.0,
    val year: Int = 0
)

