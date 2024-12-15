package ru.nyakshoot.aston_intensive_1

import android.content.Context
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ru.nyakshoot.aston_intensive_1.services.MusicPlayerService

class MainActivityViewModel : ViewModel() {

    private var currentTrackIndex = 0
    private val playlist = listOf(
        R.raw.duran_duran_invisible,
        R.raw.home_resonance
    )

    private val _currentTrackName = MutableLiveData("")
    val currentTrackName: LiveData<String> = _currentTrackName

    private val _isPlaying = MutableLiveData(false)
    val isPlaying: LiveData<Boolean> = _isPlaying

    fun initializePlayer(context: Context) {
        _currentTrackName.value = getTrackName(context, playlist[currentTrackIndex])
    }

    fun togglePlayPause(context: Context) {
        val intent = Intent(context, MusicPlayerService::class.java).apply {
            action =
                if (_isPlaying.value == true) MusicPlayerService.PAUSE
                else MusicPlayerService.PLAY
        }
        context.startForegroundService(intent)
        _isPlaying.value = !(_isPlaying.value ?: false)
    }

    fun updatePlayingState(isPlaying: Boolean) {
        _isPlaying.value = isPlaying
    }

    fun nextTrack(context: Context) {
        if (playlist.isEmpty()) return
        currentTrackIndex = (currentTrackIndex + 1) % playlist.size
        _currentTrackName.value = getTrackName(context, playlist[currentTrackIndex])

        val intent = Intent(context, MusicPlayerService::class.java).apply {
            action = MusicPlayerService.NEXT
        }
        context.startForegroundService(intent)
    }

    fun previousTrack(context: Context) {
        if (playlist.isEmpty()) return
        currentTrackIndex = if (currentTrackIndex > 0) currentTrackIndex - 1 else playlist.size - 1
        _currentTrackName.value = getTrackName(context, playlist[currentTrackIndex])

        val intent = Intent(context, MusicPlayerService::class.java).apply {
            action = MusicPlayerService.PREVIOUS
        }
        context.startForegroundService(intent)
    }

    private fun getTrackName(context: Context, resourceId: Int): String {
        return context.resources.getResourceEntryName(resourceId)
    }
}