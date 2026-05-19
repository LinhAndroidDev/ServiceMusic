package com.example.serviceandroid.fragment.radio

import androidx.lifecycle.ViewModel
import com.example.serviceandroid.model.Radio
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RadioViewModel @Inject constructor() : ViewModel() {

    fun getDemoRadioStations(): ArrayList<Radio> {
        val list = arrayListOf<Radio>()
        repeat(10) { list.add(Radio()) }
        return list
    }
}
