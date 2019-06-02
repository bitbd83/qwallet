package org.qwallet.ui

import androidx.recyclerview.widget.RecyclerView
import android.text.format.DateUtils
import android.view.View
import kotlinx.android.synthetic.main.transaction_item.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kethereum.functions.getTokenRelevantTo
import org.kethereum.functions.getTokenRelevantValue
import org.ligi.kaxt.setVisibility
import org.qwallet.R
import org.qwallet.activities.getTransactionActivityIntentForHash
import org.qwallet.data.AppDatabase
import org.qwallet.data.addressbook.resolveNameAsync
import org.qwallet.data.config.Settings
import org.qwallet.data.exchangerate.ExchangeRateProvider
import org.qwallet.data.networks.NetworkDefinitionProvider
import org.qwallet.data.tokens.getRootTokenForChain
import org.qwallet.data.transactions.TransactionEntity
import org.qwallet.ui.valueview.ValueViewController

class TransactionViewHolder(itemView: View,
                            private val direction: TransactionAdapterDirection,
                            val networkDefinitionProvider: NetworkDefinitionProvider,
                            private val exchangeRateProvider: ExchangeRateProvider,
                            val settings: Settings) : RecyclerView.ViewHolder(itemView) {


    private val amountViewModel by lazy {
        ValueViewController(itemView.difference, exchangeRateProvider, settings)
    }

    fun bind(transactionWithState: TransactionEntity?, appDatabase: AppDatabase) {

        if (transactionWithState != null) {
            val transaction = transactionWithState.transaction

            val relevantAddress = if (direction == TransactionAdapterDirection.INCOMING) {
                transaction.from
            } else {
                transaction.getTokenRelevantTo() ?: transaction.to
            }

            val tokenValue = transaction.getTokenRelevantValue()
            if (tokenValue != null) {
                val tokenAddress = transaction.to
                if (tokenAddress != null) {
                    { appDatabase.tokens.forAddress(tokenAddress) }.asyncAwaitNonNull { token ->
                        amountViewModel.setValue(tokenValue, token)
                    }
                }
            } else {
                amountViewModel.setValue(transaction.value, getRootTokenForChain(networkDefinitionProvider.getCurrent()))
            }

            relevantAddress?.let {
                appDatabase.addressBook.resolveNameAsync(it) { name ->
                    itemView.address.text = name
                }
            }

            itemView.transaction_err.setVisibility(transactionWithState.transactionState.error != null)
            if (transactionWithState.transactionState.error != null) {
                itemView.transaction_err.text = transactionWithState.transactionState.error
            }

            val epochMillis = (transaction.creationEpochSecond ?: 0) * 1000L
            val context = itemView.context
            itemView.date.text = DateUtils.getRelativeDateTimeString(context, epochMillis,
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.WEEK_IN_MILLIS,
                    0
            )

            itemView.transaction_state_indicator.setImageResource(
                    when {
                        !transactionWithState.transactionState.isPending -> R.drawable.ic_lock_black_24dp
                        transactionWithState.signatureData == null -> R.drawable.ic_lock_open_black_24dp
                        else -> R.drawable.ic_lock_outline_24dp
                    }
            )

            itemView.isClickable = true
            itemView.setOnClickListener {
                transactionWithState.hash.let {
                    context.startActivity(context.getTransactionActivityIntentForHash(it))
                }

            }
        }
    }

}

fun <T> (() -> T).asyncAwait(resultCall: (T) -> Unit) {
    GlobalScope.launch(Dispatchers.Main) {
        resultCall(withContext(Dispatchers.Default) {
            invoke()
        })
    }
}


fun <T> (() -> T?).asyncAwaitNonNull(resultCall: (T) -> Unit) {
    GlobalScope.launch(Dispatchers.Main) {
        withContext(Dispatchers.Default) {
            invoke()
        }?.let { resultCall(it) }
    }
}