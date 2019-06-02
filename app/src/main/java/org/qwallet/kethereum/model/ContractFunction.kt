package org.qwallet.kethereum.model

data class ContractFunction(
        val hexSignature: String,
        val textSignature: String? = null,
        val name: String? = null,
        val arguments: List<String>? = null)