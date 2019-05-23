package org.walleth.workers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import okhttp3.OkHttpClient
import org.kethereum.functions.encodeRLP
import org.kethereum.model.ChainId
import org.kethereum.rpc.EthereumRPC
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import org.ligi.tracedroid.logging.Log
import org.walleth.data.AppDatabase
import org.walleth.data.KEY_TX_HASH
import org.walleth.data.networks.NetworkDefinition
import org.walleth.data.networks.findNetworkDefinition
import org.walleth.data.transactions.TransactionEntity
import org.walleth.data.transactions.setHash
import org.walleth.khex.toHexString

fun ChainId.getRPCEndpoint() =
        findNetworkDefinition()?.getRPCEndpoint()

fun NetworkDefinition.getRPCEndpoint() =
        rpcEndpoints.random().replace("\${INFURA_API_KEY}", "5360ec7f835141b0bfa74eb029b61425")

class RelayTransactionWorker(appContext: Context, workerParams: WorkerParameters)
    : Worker(appContext, workerParams), KoinComponent {


    private val okHttpClient: OkHttpClient by inject()
    private val appDatabase: AppDatabase by inject()

    override fun doWork(): Result {

        val txHash = inputData.getString(KEY_TX_HASH)
        val transaction = txHash?.let { appDatabase.transactions.getByHash(it) }

        if (transaction == null) {
            Log.i("Cannot load address with $txHash")
            return Result.failure()
        }

        val chain = transaction.transaction.chain?.let { ChainId(it) }
        val baseURL = chain?.getRPCEndpoint()

        if (baseURL == null) {
            transaction.setError("RPC url not found for chain $chain")
            return Result.failure()
        } else {
            val rpc = EthereumRPC(baseURL, okHttpClient)


            val result = rpc.sendRawTransaction(transaction.transaction.encodeRLP(transaction.signatureData).toHexString())

            if (result != null) {
                if (result.error?.message != null) {
                    return if (result.error?.message != "Transaction with the same hash was already imported.") {
                        transaction.transactionState.eventLog = transaction.transactionState.eventLog ?: "" + "ERROR: ${result.error?.message}\n"

                        transaction.setError(result.error?.message)
                        appDatabase.transactions.upsert(transaction)

                        Result.failure()
                    } else {
                        transaction.setError(result.error?.message)
                        Result.retry()
                    }


                } else {
                    val newHash = result.result
                    val oldHash = transaction.hash
                    transaction.setHash(if (!newHash.startsWith("0x")) "0x$newHash" else newHash)

                    transaction.transactionState.eventLog = transaction.transactionState.eventLog ?: "" + "relayed via ${rpc.baseURL}"
                    transaction.transactionState.relayed = rpc.baseURL

                    appDatabase.transactions.deleteByHash(oldHash)
                    appDatabase.transactions.upsert(transaction)
                    return Result.success()
                }
            } else {
                transaction.setError( "Could not relay transaction")
            }
        }

        return Result.retry()
    }

    private fun TransactionEntity.setError(message: String?) {
        transactionState.error = message
        appDatabase.transactions.upsert(this)
    }
}
