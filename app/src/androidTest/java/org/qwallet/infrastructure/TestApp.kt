package org.qwallet.infrastructure

import androidx.room.Room
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import org.kethereum.keystore.api.KeyStore
import org.koin.android.viewmodel.ext.koin.viewModel
import org.koin.dsl.module.module
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.qwallet.App
import org.qwallet.contracts.FourByteDirectory
import org.qwallet.data.AppDatabase
import org.qwallet.data.DEFAULT_GAS_PRICE
import org.qwallet.data.config.Settings
import org.qwallet.data.exchangerate.ExchangeRateProvider
import org.qwallet.data.networks.CurrentAddressProvider
import org.qwallet.data.networks.NetworkDefinitionProvider
import org.qwallet.data.syncprogress.SyncProgressProvider
import org.qwallet.data.syncprogress.WallethSyncProgress
import org.qwallet.data.tokens.CurrentTokenProvider
import org.qwallet.kethereum.model.ContractFunction
import org.qwallet.testdata.DefaultCurrentAddressProvider
import org.qwallet.testdata.FixedValueExchangeProvider
import org.qwallet.testdata.TestKeyStore
import org.qwallet.viewmodels.TransactionListViewModel

private fun <T> any(): T {
    Mockito.any<T>()
    return uninitialized()
}

private fun <T> uninitialized(): T = null as T

class TestApp : App() {

    override fun createKoin() = module {
        single { fixedValueExchangeProvider as ExchangeRateProvider }
        single {
            SyncProgressProvider().apply {
                value = WallethSyncProgress(true, 42000, 42042)
            }
        }
        single { keyStore as KeyStore }
        single { mySettings }
        single { currentAddressProvider as CurrentAddressProvider }
        single { networkDefinitionProvider }
        single { currentTokenProvider }
        single { testDatabase }
        single { testFourByteDirectory }

        viewModel { TransactionListViewModel(this@TestApp, get(),get(),get()) }
    }

    override fun executeCodeWeWillIgnoreInTests() = Unit
    override fun onCreate() {
        resetDB(this)
        super.onCreate()
    }

    companion object {
        val fixedValueExchangeProvider = FixedValueExchangeProvider()
        val keyStore = TestKeyStore()
        val mySettings = mock(Settings::class.java).apply {
            `when`(currentFiat).thenReturn("EUR")
            `when`(getNightMode()).thenReturn(MODE_NIGHT_YES)
            `when`(onboardingDone).thenReturn(true)
            `when`(chain).thenReturn(4L)
            `when`(isLightClientWanted()).thenReturn(false)
            `when`(addressInitVersion).thenReturn(0)
            `when`(tokensInitVersion).thenReturn(0)
            `when`(getGasPriceFor(any())).thenReturn(DEFAULT_GAS_PRICE)
        }
        val currentAddressProvider = DefaultCurrentAddressProvider(mySettings, keyStore)
        val networkDefinitionProvider = NetworkDefinitionProvider(mySettings)
        val currentTokenProvider = CurrentTokenProvider(networkDefinitionProvider)

        val contractFunctionTextSignature1 = "aFunctionCall1(address)"
        val contractFunctionTextSignature2 = "aFunctionCall2(address)"
        val testFourByteDirectory = mock(FourByteDirectory::class.java).apply {
            `when`(getSignaturesFor(any())).then { invocation ->
                listOf(
                        ContractFunction(invocation.arguments[0] as String, textSignature = contractFunctionTextSignature1),
                        ContractFunction(invocation.arguments[0] as String, textSignature = contractFunctionTextSignature2)
                )
            }
        }
        lateinit var testDatabase: AppDatabase
        fun resetDB(context: Context) {
            testDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        }

    }
}
