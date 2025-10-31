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
    private lateinit var backPressedCallback: OnBackPressedCallback
    private lateinit var notificationDb: NotificationDatabase
    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
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
        requestNotificationPermissionIfNeeded()
        notificationDb = NotificationDatabase(this)
        NotificationDatabase.addListener(this)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
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

    override fun onNotificationChanged() {
        runOnUiThread {
            updateNotificationBadge()
        }
    }

    override fun onResume() {
        super.onResume()
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
                    NotificationHelper.createNotificationChannel(this)
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            NotificationHelper.createNotificationChannel(this)
        }
    }

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
        NotificationDatabase.removeListener(this)
        if (this::backPressedCallback.isInitialized) backPressedCallback.remove()
    }

}