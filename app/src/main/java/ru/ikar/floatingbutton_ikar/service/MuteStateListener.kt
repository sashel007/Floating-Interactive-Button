package ru.ikar.floatingbutton_ikar.service

interface MuteStateListener {
    fun onMusic()
    fun offMusic()
    fun isMuted(): Boolean
}