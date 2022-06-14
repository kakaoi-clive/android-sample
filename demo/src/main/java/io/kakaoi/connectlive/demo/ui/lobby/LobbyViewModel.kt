package io.kakaoi.connectlive.demo.ui.lobby

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LobbyViewModel : ViewModel() {

    private val _text = MutableLiveData("This is lobby Fragment")

    val text: LiveData<String> get() = _text
}