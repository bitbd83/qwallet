package org.qwallet.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import org.ligi.kaxt.livedata.CombinatorMediatorLiveData
import org.qwallet.data.AppDatabase
import org.qwallet.data.networks.CurrentAddressProvider
import org.qwallet.data.networks.NetworkDefinitionProvider
import org.qwallet.data.transactions.TransactionEntity
import org.qwallet.kethereum.model.AddressOnChain

class TransactionListViewModel(app: Application,
                               appDatabase: AppDatabase,
                               currentAddressProvider: CurrentAddressProvider,
                               networkDefinitionProvider: NetworkDefinitionProvider) : AndroidViewModel(app) {


    val isOnboardingVisible = MutableLiveData<Boolean>().apply { value = false }

    var hasIncoming = MutableLiveData<Boolean>().apply { value = false }
    var hasOutgoing = MutableLiveData<Boolean>().apply { value = false }

    val isEmptyViewVisible = CombinatorMediatorLiveData(listOf(isOnboardingVisible, hasIncoming, hasOutgoing)) {
        (isOnboardingVisible.value == false) && (hasIncoming.value == false) && (hasOutgoing.value == false)
    }

    private val addressOnChainMediator = CombinatorMediatorLiveData(listOf(currentAddressProvider, networkDefinitionProvider)) {
        AddressOnChain(currentAddressProvider.getCurrentNeverNull(), networkDefinitionProvider.getCurrent().chain)
    }

    val incomingLiveData: LiveData<PagedList<TransactionEntity>> = Transformations.switchMap(addressOnChainMediator) { addressOnChain ->
        val incomingDataSource = appDatabase.transactions.getIncomingPaged(addressOnChain.address, addressOnChain.chain.id.value)
        LivePagedListBuilder<Int, TransactionEntity>(incomingDataSource, 50).build()
    }

    val outgoingLiveData: LiveData<PagedList<TransactionEntity>> = Transformations.switchMap(addressOnChainMediator) { addressOnChain ->
        val outgoingDataSourceDataSource = appDatabase.transactions.getOutgoingPaged(addressOnChain.address, addressOnChain.chain.id.value)
        LivePagedListBuilder<Int, TransactionEntity>(outgoingDataSourceDataSource, 50).build()
    }

}