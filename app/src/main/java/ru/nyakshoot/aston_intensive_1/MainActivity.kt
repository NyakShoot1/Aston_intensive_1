package ru.nyakshoot.aston_intensive_1

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import ru.nyakshoot.aston_intensive_1.databinding.ActivityMainBinding
import ru.nyakshoot.aston_intensive_1.services.MusicPlayerService
import ru.nyakshoot.aston_intensive_1.utils.MusicServiceConstants.PLAYBACK_STATUS_ACTION
import ru.nyakshoot.aston_intensive_1.utils.MusicServiceConstants.PLAYBACK_STATUS_EXTRA


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainActivityViewModel
    private lateinit var broadcastReceiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        viewModel = ViewModelProvider(this)[MainActivityViewModel::class.java]

        viewModel.currentTrackName.observe(this) { trackName ->
            binding.nameTrackTextView.text = trackName
        }

        viewModel.isPlaying.observe(this) { isPlaying ->
            binding.playStopBtn.setImageResource(
                if (isPlaying) R.drawable.baseline_pause_circle_24
                else R.drawable.baseline_play_circle_24
            )
        }

        viewModel.initializePlayer(this)

        binding.playStopBtn.setOnClickListener {
            viewModel.togglePlayPause(this)
        }

        binding.previousBtn.setOnClickListener {
            viewModel.previousTrack(this)
        }

        binding.nextBtn.setOnClickListener {
            viewModel.nextTrack(this)
        }

        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val isPlaying = intent?.getBooleanExtra(
                    PLAYBACK_STATUS_EXTRA,
                    false
                ) ?: false
                viewModel.updatePlayingState(isPlaying)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onStart() {
        super.onStart()
        checkNotificationPermission()

        val intentFilter = IntentFilter(PLAYBACK_STATUS_ACTION)
        registerReceiver(broadcastReceiver, intentFilter, RECEIVER_NOT_EXPORTED)

        val intent = Intent(this, MusicPlayerService::class.java)
        startForegroundService(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
    }

    private fun checkNotificationPermission() {
        if (SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }
    }
}