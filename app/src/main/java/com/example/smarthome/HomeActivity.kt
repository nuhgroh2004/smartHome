package com.example.smarthome

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.smarthome.databinding.ActivityHomeBinding
import android.view.View
import android.animation.ObjectAnimator
import android.animation.AnimatorSet

class HomeActivity : AppCompatActivity(), NotificationDatabase.OnNotificationChangeListener {
    private lateinit var binding: ActivityHomeBinding
    private var doubleBackToExitPressedOnce = false

    // Add dispatcher callback reference
    private lateinit var backPressedCallback: OnBackPressedCallback
    private lateinit var notificationDb: NotificationDatabase

    // Permission launcher for POST_NOTIFICATIONS (Android 13+)
    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                // Ensure notification channel exists
                NotificationHelper.createNotificationChannel(this)
            } else {
                Toast.makeText(this, "Izin notifikasi ditolak. Anda tidak akan menerima pemberitahuan.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        // Request notification permission on Android 13+
        requestNotificationPermissionIfNeeded()

        // Inisialisasi notification database
        notificationDb = NotificationDatabase(this)

        // Register listener untuk update real-time
        NotificationDatabase.addListener(this)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Handle system back press using OnBackPressedDispatcher
        backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (doubleBackToExitPressedOnce) {
                    finishAffinity()
                    return
                }
                doubleBackToExitPressedOnce = true
                val toast = Toast.makeText(this@HomeActivity, "Tekan 1 kali lagi untuk keluar", Toast.LENGTH_SHORT)
                toast.show()
                Handler(Looper.getMainLooper()).postDelayed({
                    doubleBackToExitPressedOnce = false
                }, 2000)
            }
        }
        onBackPressedDispatcher.addCallback(this, backPressedCallback)


        /* ------------------------ fungsional untuk tombol berpidah activity -------------------------- */
        binding.cardLampu.setOnClickListener {
            animateClick(it) {
                val intent = Intent(this, ControlLampuActivity::class.java)
                startActivity(intent)
            }
        }
        binding.cardTandonAir.setOnClickListener {
            animateClick(it) {
                val intent = Intent(this, MonitoringTandonAirActivity::class.java)
                startActivity(intent)
            }
        }
        binding.cardMonitoringRuangan.setOnClickListener {
            animateClick(it) {
                val intent = Intent(this, MonitoringRuanganActivity::class.java)
                startActivity(intent)
            }
        }
        binding.notificationBell.setOnClickListener {
            animateClick(it) {
                val intent = Intent(this, NotificationActivity::class.java)
                startActivity(intent)
            }
        }
        binding.cardMonitoringListrik.setOnClickListener {
            animateClick(it) {
                val intent = Intent(this, MonitoringListrikActivity::class.java)
                startActivity(intent)
            }
        }
        /* ------------------------ fungsional untuk tombol berpidah activity -------------------------- */
    }

    // Callback dari listener saat ada perubahan notifikasi
    override fun onNotificationChanged() {
        // Update badge di UI thread
        runOnUiThread {
            updateNotificationBadge()
        }
    }

    override fun onResume() {
        super.onResume()
        // Update badge setiap kali activity muncul
        updateNotificationBadge()
    }

    private fun updateNotificationBadge() {
        val unreadCount = notificationDb.getUnreadCount()

        if (unreadCount > 0) {
            binding.tvNotificationBadge.visibility = View.VISIBLE
            binding.tvNotificationBadge.text = if (unreadCount > 99) "99+" else unreadCount.toString()
        } else {
            binding.tvNotificationBadge.visibility = View.GONE
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED -> {
                    // Already granted
                    NotificationHelper.createNotificationChannel(this)
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // You may show a rationale to the user and then request permission
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    // Directly request permission
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // Pre Android 13: ensure channel exists
            NotificationHelper.createNotificationChannel(this)
        }
    }

    // Fungsi untuk animasi klik
    private fun animateClick(view: View, action: () -> Unit) {
        val scaleDown = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, "scaleX", 0.95f),
                ObjectAnimator.ofFloat(view, "scaleY", 0.95f)
            )
            duration = 100
        }

        val scaleUp = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, "scaleX", 1f),
                ObjectAnimator.ofFloat(view, "scaleY", 1f)
            )
            duration = 100
        }

        scaleDown.start()
        scaleDown.addListener(object : android.animation.Animator.AnimatorListener {
            override fun onAnimationStart(animation: android.animation.Animator) {}
            override fun onAnimationEnd(animation: android.animation.Animator) {
                scaleUp.start()
                Handler(Looper.getMainLooper()).postDelayed({
                    action()
                }, 50)
            }
            override fun onAnimationCancel(animation: android.animation.Animator) {}
            override fun onAnimationRepeat(animation: android.animation.Animator) {}
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister listener saat activity di destroy
        NotificationDatabase.removeListener(this)
        if (this::backPressedCallback.isInitialized) backPressedCallback.remove()
    }

}