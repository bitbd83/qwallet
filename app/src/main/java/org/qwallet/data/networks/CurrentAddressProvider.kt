package org.qwallet.data.networks

import androidx.lifecycle.MutableLiveData
import org.kethereum.model.Address
import org.qwallet.data.config.Settings

open class CurrentAddressProvider(val settings: Settings) : MutableLiveData<Address>() {

    fun setCurrent(value: Address) {
        settings.accountAddress = value.hex
        setValue(value)
    }

    fun getCurrent() = value
    fun getCurrentNeverNull() = value!!
}