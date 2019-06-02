package org.qwallet.ui

import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import android.view.LayoutInflater
import android.view.ViewGroup
import org.qwallet.R
import org.qwallet.data.AppDatabase
import org.qwallet.data.config.Settings
import org.qwallet.data.exchangerate.ExchangeRateProvider
import org.qwallet.data.networks.NetworkDefinitionProvider
import org.qwallet.data.transactions.TransactionEntity

enum class TransactionAdapterDirection {
    INCOMING, OUTGOING
}

class TransactionDiffCallback : DiffUtil.ItemCallback<TransactionEntity>() {

    override fun areItemsTheSame(oldItem: TransactionEntity, newItem: TransactionEntity): Boolean {
        return oldItem.hash == newItem.hash
    }

    override fun areContentsTheSame(oldItem: TransactionEntity, newItem: TransactionEntity): Boolean {
        return oldItem == newItem
    }
}


class TransactionRecyclerAdapter(val appDatabase: AppDatabase,
                                 private val direction: TransactionAdapterDirection,
                                 val networkDefinitionProvider: NetworkDefinitionProvider,
                                 private val exchangeRateProvider: ExchangeRateProvider,
                                 val settings: Settings
) : PagedListAdapter<TransactionEntity, TransactionViewHolder>(TransactionDiffCallback()) {

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) = holder.bind(getItem(position), appDatabase)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.transaction_item, parent, false)
        return TransactionViewHolder(itemView, direction, networkDefinitionProvider, exchangeRateProvider, settings)
    }

}