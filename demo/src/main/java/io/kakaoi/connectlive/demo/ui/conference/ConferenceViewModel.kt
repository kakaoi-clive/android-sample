package io.kakaoi.connectlive.demo.ui.conference

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ConferenceViewModel : ViewModel() {

    private val _text = MutableLiveData("This is conference Fragment")

    val text: LiveData<String> get() = _text
}