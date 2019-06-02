package org.qwallet.activities

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import com.androidadvance.topsnackbar.TSnackbar
import com.github.amlcurran.showcaseview.ShowcaseView
import com.github.amlcurran.showcaseview.SimpleShowcaseEventListener
import com.github.amlcurran.showcaseview.targets.ViewTarget
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_create_transaction.*
import kotlinx.android.synthetic.main.value.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kethereum.contract.abi.types.convertStringToABIType
import org.kethereum.eip155.extractChainID
import org.kethereum.eip155.signViaEIP155
import org.kethereum.erc681.ERC681
import org.kethereum.erc681.generateURL
import org.kethereum.erc681.parseERC681
import org.kethereum.erc831.isEthereumURLString
import org.kethereum.extensions.maybeHexToBigInteger
import org.kethereum.extensions.toHexStringZeroPadded
import org.kethereum.functions.*
import org.kethereum.keccakshortcut.keccak
import org.kethereum.keystore.api.KeyStore
import org.kethereum.methodsignatures.model.TextMethodSignature
import org.kethereum.methodsignatures.toHexSignature
import org.kethereum.model.*
import org.koin.android.ext.android.inject
import org.ligi.kaxt.doAfterEdit
import org.ligi.kaxt.setVisibility
import org.ligi.kaxt.startActivityFromURL
import org.ligi.kaxtui.alert
import org.ligi.tracedroid.logging.Log
import org.qwallet.data.AppDatabase
import org.qwallet.R
import org.qwallet.activities.nfc.startNFCSigningActivity
import org.qwallet.activities.qrscan.startScanActivityForResult
import org.qwallet.activities.trezor.TREZOR_REQUEST_CODE
import org.qwallet.activities.trezor.startTrezorActivity
import org.qwallet.data.*
import org.qwallet.data.addressbook.AddressBookEntry
import org.qwallet.data.addressbook.getByAddressAsync
import org.qwallet.data.addressbook.getSpec
import org.qwallet.data.addressbook.resolveNameAsync
import org.qwallet.data.balances.Balance
import org.qwallet.data.config.Settings
import org.qwallet.data.exchangerate.ExchangeRateProvider
import org.qwallet.data.networks.CurrentAddressProvider
import org.qwallet.data.networks.NetworkDefinitionProvider
import org.qwallet.data.tokens.*
import org.qwallet.data.transactions.TransactionState
import org.qwallet.data.transactions.toEntity
import org.qwallet.kethereum.android.TransactionParcel
import org.walleth.khex.hexToByteArray
import org.walleth.khex.toHexString
import org.walleth.khex.toNoPrefixHexString
import org.qwallet.model.ACCOUNT_TYPE_MAP
import org.qwallet.ui.asyncAwait
import org.qwallet.ui.chainIDAlert
import org.qwallet.ui.valueview.ValueViewController
import org.qwallet.util.hasText
import org.qwallet.util.question
import org.qwallet.util.security.getPasswordForAccountType
import java.math.BigInteger
import java.math.BigInteger.ONE
import java.math.BigInteger.ZERO
import java.util.*

const val TO_ADDRESS_REQUEST_CODE = 1
const val FROM_ADDRESS_REQUEST_CODE = 2
const val TOKEN_REQUEST_CODE = 3

class CreateTransactionActivity : BaseSubActivity() {

    private var currentERC681: ERC681 = ERC681()
    private var currentToAddress: Address? = null

    private val currentAddressProvider: CurrentAddressProvider by inject()
    private val networkDefinitionProvider: NetworkDefinitionProvider by inject()
    private val currentTokenProvider: CurrentTokenProvider by inject()
    private val keyStore: KeyStore by inject()
    private val appDatabase: AppDatabase by inject()
    private val settings: Settings by inject()
    private val exchangeRateProvider: ExchangeRateProvider by inject()

    private var currentBalance: Balance? = null
    private var lastWarningURI: String? = null
    private var currentBalanceLive: LiveData<Balance>? = null
    private var currentSignatureData: SignatureData? = null
    private var currentTxHash: String? = null

    private var currentShowCase: ShowcaseView? = null
    private var currentTopSnackBar: TSnackbar? = null

    private var currentAccount: AddressBookEntry? = null

    private val amountController by lazy {
        ValueViewController(amount_value, exchangeRateProvider, settings)
    }
    private val feeValueViewModel by lazy {
        ValueViewController(fee_value_view, exchangeRateProvider, settings)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            TREZOR_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    if (data?.hasExtra("TXHASH") == true) {
                        currentTxHash = data.getStringExtra("TXHASH")
                    }
                    storeDefaultGasPriceAndFinish()
                }
            }

            REQUEST_CODE_ENTER_PASSWORD -> {
                if (resultCode == Activity.RESULT_OK) {
                    startTransaction(data?.getStringExtra(EXTRA_KEY_PWD), createTransaction())
                }
            }
            TOKEN_REQUEST_CODE -> {
                onCurrentTokenChanged()
            }
            else -> data?.let {
                if (data.hasExtra(EXTRA_KEY_ADDRESS)) {
                    setToFromURL(data.getStringExtra(EXTRA_KEY_ADDRESS), fromUser = true)
                } else if (data.hasExtra("SCAN_RESULT")) {
                    setToFromURL(data.getStringExtra("SCAN_RESULT"), fromUser = true)
                }
            }
        }


    }

    private fun String.toERC681() = if (startsWith("0x")) ERC681(address = this) else parseERC681(this)

    private fun isParityFlow() = intent.getBooleanExtra("parityFlow", false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_create_transaction)

        currentERC681 = if (savedInstanceState != null && savedInstanceState.containsKey("ERC67")) {
            savedInstanceState.getString("ERC67")
        } else {
            intent.data?.toString()
        }?.toERC681() ?: ERC681()

        if (savedInstanceState != null && savedInstanceState.containsKey("lastERC67")) {
            lastWarningURI = savedInstanceState.getString("lastERC67")
        }

        networkDefinitionProvider.observe(this, Observer {
            supportActionBar?.subtitle = getString(R.string.create_transaction_on_network_subtitle, networkDefinitionProvider.getCurrent().getNetworkName())
        })

        currentTokenProvider.observe(this, Observer {
            onCurrentTokenChanged()
        })

        currentAddressProvider.observe(this, Observer { address ->
            address?.let {
                appDatabase.addressBook.getByAddressAsync(address) { entry ->
                    currentAccount = entry
                    from_address.text = entry?.name
                    val drawable = ACCOUNT_TYPE_MAP[entry.getSpec()?.type]?.actionDrawable

                    fab.setImageResource(drawable ?: R.drawable.ic_action_done)
                    fab.setOnClickListener {
                        onFabClick()
                    }
                }
            }
        })

        current_token_symbol.setOnClickListener {
            startActivityForResult(Intent(this, SelectTokenActivity::class.java), TOKEN_REQUEST_CODE)
        }

        gas_price_input.setText(if (intent.getStringExtra("gasPrice") != null) {
            intent.getStringExtra("gasPrice").maybeHexToBigInteger().toString()
        } else {
            settings.getGasPriceFor(networkDefinitionProvider.getCurrent()).toString()
        })

        intent.getStringExtra("data")?.let {
            val data = it.hexToByteArray()

            if (data.toList().startsWith(tokenTransferSignature)) {
                currentERC681.function = "transfer"

                Log.i("TXData" + data.toHexString())

                val tmpTX = Transaction().apply {
                    input = data
                }

                currentERC681.functionParams = listOf(
                        "address" to tmpTX.getTokenTransferTo().hex,
                        "uint256" to tmpTX.getTokenTransferValue().toString()
                )
            }

        }

        sweep_button.setOnClickListener {
            val balance = currentBalanceSafely()
            if (currentTokenProvider.getCurrent().isRootToken()) {
                val amountAfterFee = balance - calculateGasCost()
                if (amountAfterFee < ZERO) {
                    alert(R.string.no_funds_after_fee)
                } else {
                    amountController.setValue(amountAfterFee, currentTokenProvider.getCurrent())
                }
            } else {
                amountController.setValue(balance, currentTokenProvider.getCurrent())
            }
        }

        gas_limit_input.doAfterEdit {
            refreshFee()
        }

        gas_price_input.doAfterEdit {
            refreshFee()
        }

        gas_station_image.setOnClickListener {
            startActivityFromURL("http://ethgasstation.info")
        }

        show_advanced_button.setOnClickListener {
            show_advanced_button.visibility = View.GONE
            fee_label.visibility = View.VISIBLE
            fee_value_view.visibility = View.VISIBLE
            gas_price_input_container.visibility = View.VISIBLE
            gas_limit_input_container.visibility = View.VISIBLE
            nonce_title.visibility = View.VISIBLE
            nonce_input_container.visibility = View.VISIBLE
        }

        Transformations.switchMap(currentAddressProvider) { address ->
            appDatabase.transactions.getNonceForAddressLive(address, networkDefinitionProvider.getCurrent().chain.id.value)
        }.observe(this, Observer {

            if (intent.getStringExtra("nonce") == null) {
                val nonceBigInt = if (it != null && !it.isEmpty()) {
                    it.max()!! + ONE
                } else {
                    ZERO
                }
                nonce_input.setText(String.format(Locale.ENGLISH, "%d", nonceBigInt))
            }

        })
        refreshFee()
        setToFromURL(currentERC681.generateURL(), false)

        address_list_button.setOnClickListener {
            currentShowCase?.hide()
            val intent = Intent(this@CreateTransactionActivity, AccountPickActivity::class.java)
            startActivityForResult(intent, TO_ADDRESS_REQUEST_CODE)
        }

        from_address_list_button.setOnClickListener {
            val intent = Intent(this@CreateTransactionActivity, AccountPickActivity::class.java)
            startActivityForResult(intent, FROM_ADDRESS_REQUEST_CODE)
        }

        val functionVisibility = currentERC681.function != null && !currentERC681.isTokenTransfer()
        function_label.setVisibility(functionVisibility)
        function_text.setVisibility(functionVisibility)

        if (functionVisibility) {
            function_text.text = currentERC681.function + "(" + currentERC681.functionParams?.joinToString(",") { it.second } + ")"
        }
    }

    private fun onCurrentTokenChanged() {
        val currentToken = currentTokenProvider.getCurrent()
        currentBalanceLive = Transformations.switchMap(currentAddressProvider) { address ->
            appDatabase.balances.getBalanceLive(address, currentToken.address, networkDefinitionProvider.getCurrent().chain.id.value)
        }
        currentBalanceLive!!.observe(this, Observer {
            currentBalance = it
        })

        amountController.setValue(amountController.getValueOrZero(), currentToken)

        if (currentToken.isRootToken()) {
            gas_limit_input.setText(DEFAULT_GAS_LIMIT_ETH_TX.toString())
        } else {
            gas_limit_input.setText(DEFAULT_GAS_LIMIT_ERC_20_TX.toString())
        }
    }

    private fun onFabClick() {
        if (to_address.text.isEmpty() || currentToAddress == null) {

            currentShowCase = ShowcaseView.Builder(this)
                    .setTarget(ViewTarget(R.id.address_list_button, this))
                    .setShowcaseEventListener(object : SimpleShowcaseEventListener() {
                        override fun onShowcaseViewHide(showcaseView: ShowcaseView?) {
                            processShowCaseViewState(false)
                            currentTopSnackBar?.dismiss()
                        }
                    })
                    .build()

            currentShowCase?.show()

            processShowCaseViewState(true)

            currentTopSnackBar = TSnackbar.make(fab, getString(R.string.create_tx_err), TSnackbar.LENGTH_INDEFINITE).apply {
                setAction(android.R.string.ok) {
                    currentTopSnackBar?.dismiss()
                }
                if (Build.VERSION.SDK_INT >= 21) {
                    setIconPadding(18)
                    setIconLeft(R.drawable.ic_warning_orange_24dp, 24f)
                }
                also {
                    val textView: TextView = it.view.findViewById(com.androidadvance.topsnackbar.R.id.snackbar_text)
                    textView.setTextColor(Color.WHITE)
                }
                show()
            }

        } else if (currentTokenProvider.getCurrent().isRootToken() && hasEnoughETH()) {
            alert(R.string.create_tx_error_not_enough_funds)
        } else if (!nonce_input.hasText()) {
            alert(title = R.string.nonce_invalid, message = R.string.please_enter_name)
        } else {
            if (currentTokenProvider.getCurrent().isRootToken() && currentERC681.function == null && amountController.getValueOrZero() == ZERO) {
                question(R.string.create_tx_zero_amount, R.string.alert_problem_title, DialogInterface.OnClickListener { _, _ -> prepareTransaction() })
            } else if (!currentTokenProvider.getCurrent().isRootToken() && amountController.getValueOrZero() > currentBalanceSafely()) {
                question(R.string.create_tx_negative_token_balance, R.string.alert_problem_title, DialogInterface.OnClickListener { _, _ -> prepareTransaction() })
            } else {
                prepareTransaction()
            }
        }
    }

    private fun calculateGasCost() = gas_price_input.asBigInit() * gas_limit_input.asBigInit()
    private fun hasEnoughETH() = amountController.getValueOrZero() + calculateGasCost() > currentBalanceSafely()

    private fun processShowCaseViewState(isShowcaseViewShown: Boolean) {
        if (isShowcaseViewShown) fab.hide() else fab.show()
        show_advanced_button.isEnabled = !isShowcaseViewShown
        amountController.setEnabled(!isShowcaseViewShown)
    }

    private fun prepareTransaction() {
        when (val type = currentAccount.getSpec()?.type) {
            ACCOUNT_TYPE_PIN_PROTECTED, ACCOUNT_TYPE_BURNER, ACCOUNT_TYPE_PASSWORD_PROTECTED -> getPasswordForAccountType(type) { pwd ->
                if (pwd != null) {
                    startTransaction(pwd, createTransaction())
                }
            }
            ACCOUNT_TYPE_NFC -> startNFCSigningActivity(TransactionParcel(createTransaction()))
            ACCOUNT_TYPE_TREZOR -> startTrezorActivity(TransactionParcel(createTransaction()))
        }
    }

    private fun startTransaction(password: String?, transaction: Transaction) {
        GlobalScope.launch(Dispatchers.Main) {

            fab_progress_bar.visibility = View.VISIBLE
            fab.isEnabled = false

            val error: String? = withContext(Dispatchers.Default) {
                try {
                    val currentAddress = currentAddressProvider.getCurrentNeverNull()
                    val signatureData = keyStore.getKeyForAddress(currentAddress, password ?: DEFAULT_PASSWORD)?.let {
                        Snackbar.make(fab, "Signing transaction", Snackbar.LENGTH_INDEFINITE).show()
                        transaction.signViaEIP155(it, networkDefinitionProvider.getCurrent().chain)
                    }

                    currentSignatureData = signatureData

                    currentTxHash = transaction.encodeRLP(signatureData).keccak().toHexString()
                    transaction.txHash = currentTxHash


                    val entity = transaction.toEntity(signatureData = signatureData, transactionState = TransactionState())
                    appDatabase.transactions.upsert(entity)
                    null
                } catch (e: Exception) {
                    e.message
                }
            }

            fab_progress_bar.visibility = View.INVISIBLE
            fab.isEnabled = true

            if (error != null) {
                alert("Could not sign transaction: $error")
            } else {
                storeDefaultGasPriceAndFinish()
            }
        }
    }

    private fun createTransaction(): Transaction {

        val localERC681 = currentERC681

        val transaction = (if (currentTokenProvider.getCurrent().isRootToken()) createTransactionWithDefaults(
                value = amountController.getValueOrZero(),
                to = currentToAddress!!,
                from = currentAddressProvider.getCurrentNeverNull()
        ) else createTransactionWithDefaults(
                creationEpochSecond = System.currentTimeMillis() / 1000,
                value = ZERO,
                to = currentTokenProvider.getCurrent().address,
                from = currentAddressProvider.getCurrentNeverNull(),
                input = createTokenTransferTransactionInput(currentToAddress!!, amountController.getValueOrZero())
        )).copy(chain = networkDefinitionProvider.getCurrent().chain.id.value, creationEpochSecond = System.currentTimeMillis() / 1000)


        if (currentTokenProvider.getCurrent().isRootToken() && localERC681.function != null) {
            val parameterSignature = localERC681.functionParams.joinToString(",") { it.first }
            val functionSignature = TextMethodSignature(localERC681.function + "($parameterSignature)")

            val parameterContent = localERC681.functionParams.joinToString("") {
                convertStringToABIType(it.first).apply {
                    parseValueFromString(it.second)
                }.toBytes().toNoPrefixHexString()
            }
            transaction.input = (functionSignature.toHexSignature().hex + parameterContent).hexToByteArray()
        }

        transaction.nonce = nonce_input.asBigInit()
        transaction.gasPrice = gas_price_input.asBigInit()
        transaction.gasLimit = gas_limit_input.asBigInit()

        return transaction
    }

    private fun currentBalanceSafely() = currentBalance?.balance ?: ZERO

    private fun TextView.asBigInit() = BigInteger(text.toString())

    private fun refreshFee() {
        val fee = try {
            BigInteger(gas_price_input.text.toString()) * BigInteger(gas_limit_input.text.toString())
        } catch (numberFormatException: NumberFormatException) {
            ZERO
        }
        feeValueViewModel.setValue(fee, getRootTokenForChain(networkDefinitionProvider.getCurrent()))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("ERC67", currentERC681.generateURL())
        outState.putString("lastERC67", lastWarningURI)
        super.onSaveInstanceState(outState)
    }

    private fun setToFromURL(uri: String?, fromUser: Boolean) {
        if (uri != null) {

            val localERC681 = uri.toERC681()
            currentERC681 = localERC681

            if (currentERC681.valid) {

                chainIDAlert(networkDefinitionProvider,
                        localERC681.chainId?.let { ChainId(it) },
                        continuationWithWrongChainId = {
                            finish()
                        },
                        continuationWithCorrectOrNullChainId = {

                            intent.getStringExtra("nonce")?.let {
                                nonce_input.setText(it.maybeHexToBigInteger().toString())
                            }

                            currentToAddress = localERC681.getToAddress()?.apply {
                                to_address.text = this.hex
                                appDatabase.addressBook.resolveNameAsync(this) {
                                    to_address.text = it
                                }
                            }

                            if (localERC681.isTokenTransfer()) {
                                if (localERC681.address != null) {
                                    { appDatabase.tokens.forAddress(Address(localERC681.address!!)) }.asyncAwait { token ->
                                        if (token != null) {

                                            if (token != currentTokenProvider.getCurrent()) {
                                                currentTokenProvider.setCurrent(token)
                                                currentBalanceLive?.removeObservers(this)
                                                onCurrentTokenChanged()
                                            }

                                            localERC681.getValueForTokenTransfer()?.let {
                                                amountController.setValue(it, token)
                                            }
                                        } else {
                                            alert(getString(R.string.add_token_manually, localERC681.address), getString(R.string.unknown_token))
                                        }
                                    }
                                } else {
                                    alert(getString(R.string.no_token_address), getString(R.string.unknown_token))
                                }
                            } else {

                                if (localERC681.function != null) {
                                    checkFunctionParameters(localERC681)

                                }
                                localERC681.value?.let {

                                    if (!currentTokenProvider.getCurrent().isRootToken()) {
                                        currentTokenProvider.setCurrent(getRootTokenForChain(networkDefinitionProvider.getCurrent()))
                                        currentBalanceLive?.removeObservers(this)
                                        onCurrentTokenChanged()
                                    }

                                    amountController.setValue(it, currentTokenProvider.getCurrent())
                                }
                            }

                            localERC681.gas?.let {
                                show_advanced_button.callOnClick()
                                gas_limit_input.setText(it.toString())
                            }

                        })

            } else {
                currentToAddress = null
                to_address.text = getString(R.string.no_address_selected)
                if (fromUser || lastWarningURI != uri) {
                    lastWarningURI = uri
                    if (uri.isEthereumURLString()) {
                        alert(getString(R.string.create_tx_error_invalid_url_msg, uri), getString(R.string.create_tx_error_invalid_url_title))
                    } else {
                        alert(getString(R.string.create_tx_error_invalid_address, uri))
                    }

                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        amountController.refreshNonValues()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_create_tx, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.menu_scan -> true.also { startScanActivityForResult(this) }
        else -> super.onOptionsItemSelected(item)
    }

    private fun checkFunctionParameters(localERC681: ERC681) {
        val functionToByteList = localERC681.functionParams.map {

            val type = try {
                convertStringToABIType(it.first)
            } catch (e: IllegalArgumentException) {
                null
            }

            val bytes = try {
                type?.parseValueFromString(it.second)
                type?.toBytes()
            } catch (e: NotImplementedError) {
                null
            } catch (e: IllegalArgumentException) {
                null
            }


            type to bytes
        }


        val indexOfFirstInvalidParam: Int = functionToByteList.indexOfFirst { it.first == null }

        if (indexOfFirstInvalidParam >= 0) {
            val type = localERC681.functionParams[indexOfFirstInvalidParam].first
            alert(getString(R.string.warning_invalid_param, indexOfFirstInvalidParam.toString(), type))
            return
        }

        val indexOfFirstDynamicType = functionToByteList.indexOfFirst { it.first?.isDynamic() == true }
        if (indexOfFirstDynamicType >= 0) {
            val type = localERC681.functionParams[indexOfFirstDynamicType].first
            alert(getString(R.string.warning_dynamic_length_params_unsupported, indexOfFirstDynamicType.toString(), type))
            return
        }

        val indexOfFirsInvalidParameter = functionToByteList.indexOfFirst { it.second == null }
        if (indexOfFirsInvalidParameter >= 0) {
            val parameter = localERC681.functionParams[indexOfFirsInvalidParameter]
            val type = parameter.first
            val value = parameter.second
            alert(getString(R.string.warning_problem_with_parameter, indexOfFirsInvalidParameter.toString(), type, value))
            return
        }
    }

    private fun storeDefaultGasPriceAndFinish() {
        val gasPrice = gas_price_input.asBigInit()
        val networkDefinition = networkDefinitionProvider.getCurrent()
        if (gasPrice != settings.getGasPriceFor(networkDefinition)) {
            AlertDialog.Builder(this)
                    .setTitle(getString(R.string.default_gas_price, networkDefinition.getNetworkName()))
                    .setMessage(R.string.store_gas_price)
                    .setPositiveButton(R.string.save) { _: DialogInterface, _: Int ->
                        settings.storeGasPriceFor(gasPrice, networkDefinition)
                        finishAndFollowUp()
                    }
                    .setNegativeButton(R.string.no) { _: DialogInterface, _: Int ->
                        finishAndFollowUp()
                    }
                    .show()
        } else {
            finishAndFollowUp()
        }
    }

    private fun finishAndFollowUp() {
        if (isParityFlow()) {
            currentSignatureData?.let {
                val hex = it.r.toHexStringZeroPadded(64, false) +
                        it.s.toHexStringZeroPadded(64, false) +
                        (it.v - 35 - (it.extractChainID() ?: 0) * 2).toBigInteger().toHexStringZeroPadded(2, false)
                val intent = Intent(this, ParitySignerQRActivity::class.java)
                        .putExtra("signatureHex", hex)
                startActivity(intent)
            }
        }
        setResult(RESULT_OK, Intent().apply { putExtra("TXHASH", currentTxHash) })
        finish()
    }
}
