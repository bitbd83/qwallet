package org.walleth.data.tokens

import androidx.room.Entity
import org.kethereum.model.Address
import org.walleth.data.networks.NetworkDefinition

fun Token.isRootToken() = address.hex == "0x0"

fun getRootTokenForChain(networkDefinition: NetworkDefinition) = Token(
        symbol = networkDefinition.tokenShortName,
        name = "Ether",
        decimals = 18,
        address = Address("0x0"),
        chain = networkDefinition.chain.id.value,
        showInList = true,
        starred = false,
        fromUser = false,
        order = 0
)

@Entity(tableName = "tokens", primaryKeys = ["address", "chain"])
data class Token(
        val name: String,
        val symbol: String,
        val address: Address,
        val decimals: Int,
        val chain: Long,
        val showInList: Boolean,
        val starred: Boolean,
        val fromUser: Boolean,
        val order:Int
)