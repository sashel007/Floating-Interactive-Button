package ru.ikar.floatingbutton_ikar.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ServiceViewModel : ViewModel() {
    val serviceData: MutableLiveData<ServiceData> = MutableLiveData()

    fun updateFromService(data: ServiceData) {
        serviceData.postValue(data)
    }
}

data class ServiceData(
    val initialX: Float,
    val initialY: Float,
    val initialTouchX: Float,
    val initialTouchY: Float
)