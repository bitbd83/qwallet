package org.qwallet.data.transactions

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.kethereum.model.Address
import java.math.BigInteger

@Dao
interface TransactionDAO {

    @Query("SELECT * FROM transactions")
    fun getTransactions(): List<TransactionEntity>

    @Query("SELECT * FROM transactions")
    fun getTransactionsLive(): LiveData<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE (\"to\" = :address COLLATE NOCASE OR \"extraIncomingAffectedAddress\" = :address COLLATE NOCASE ) AND chain=:chain ORDER BY creationEpochSecond DESC")
    fun getIncomingTransactionsForAddressOnChainOrdered(address: Address, chain: Long): LiveData<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE (\"to\" = :address COLLATE NOCASE OR \"extraIncomingAffectedAddress\" = :address COLLATE NOCASE )AND chain=:chain ORDER BY creationEpochSecond DESC")
    fun getIncomingPaged(address: Address, chain: Long): DataSource.Factory<Int, TransactionEntity>

    @Query("SELECT * FROM transactions WHERE \"from\" = :address COLLATE NOCASE AND chain=:chain ORDER BY nonce DESC")
    fun getOutgoingPaged(address: Address, chain: Long): DataSource.Factory<Int, TransactionEntity>

    @Query("SELECT * FROM transactions WHERE \"from\" = :address COLLATE NOCASE  AND chain=:chain ORDER BY nonce DESC")
    fun getOutgoingTransactionsForAddressOnChainOrdered(address: Address, chain: Long): LiveData<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE \"to\" COLLATE NOCASE IN(:addresses) OR  \"from\" COLLATE NOCASE IN(:addresses)")
    fun getAllTransactionsForAddressLive(addresses: List<Address>): LiveData<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE \"to\" COLLATE NOCASE IN(:addresses) OR  \"from\" COLLATE NOCASE IN(:addresses)")
    fun getAllTransactionsForAddress(addresses: List<Address>): List<TransactionEntity>


    @Query("SELECT * FROM transactions")
    fun getAll(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(transactionEntity: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(transactionEntities: List<TransactionEntity>)

    @Query("SELECT nonce from transactions WHERE \"from\" = :address COLLATE NOCASE AND chain=:chain")
    fun getNonceForAddressLive(address: Address, chain: Long): LiveData<List<BigInteger>>

    @Query("SELECT nonce from transactions WHERE \"from\" = :address COLLATE NOCASE AND chain=:chain")
    fun getNonceForAddress(address: Address, chain: Long): List<BigInteger>

    @Query("SELECT * from transactions WHERE r IS NOT NULL AND relayed=\"\" AND error IS NULL")
    fun getAllToRelayLive(): LiveData<List<TransactionEntity>>

    @Query("SELECT * from transactions WHERE hash = :hash COLLATE NOCASE")
    fun getByHash(hash: String): TransactionEntity?

    @Query("SELECT * from transactions WHERE hash = :hash COLLATE NOCASE")
    fun getByHashLive(hash: String): LiveData<TransactionEntity>

    @Query("DELETE FROM transactions WHERE hash = :hash COLLATE NOCASE")
    fun deleteByHash(hash: String)

    @Query("DELETE FROM transactions")
    fun deleteAll()
}