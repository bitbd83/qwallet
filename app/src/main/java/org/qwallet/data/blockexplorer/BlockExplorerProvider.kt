package org.qwallet.data.blockexplorer

import org.qwallet.data.networks.NetworkDefinitionProvider
import org.walleth.kethereum.blockscout.getBlockScoutBlockExplorer

class BlockExplorerProvider(var network: NetworkDefinitionProvider) {

    fun get() = getBlockScoutBlockExplorer(network.getCurrent().chain)

}