package org.qwallet.data.addressbook

import org.kethereum.model.Address

var ligi = AddressBookEntry(
        name = "BitBD",
        address = Address("0x5E47b8155E4D66d5A88437D471e80daEb4Df9498"),
        note = "Developer"
)

val michael = AddressBookEntry(
        name = "JK",
        address = Address("0xB2b4076a36b8993094cBfdb72512820B8BeDb6Ce"),
        note = "Icon designer"
)

val faucet = AddressBookEntry(
        name = "Rinkeby Faucet",
        address = Address("0x31b98d14007bdee637298086988a0bbd31184523"),
        note = "The source of some FREE Rinkeby ether"
)

val goerli_pusher = AddressBookEntry(
        name = "Goerli pusher",
        address = Address("0x03e0ffece04d779388b7a1d5c5102ac54bd479ee"),
        note = "Mints TST tokens for you"
)

val goerli_simple_faucet = AddressBookEntry(
        name = "Goerli simple faucet",
        address = Address("0x8ced5ad0d8da4ec211c17355ed3dbfec4cf0e5b9")
)


val goerli_social_faucet = AddressBookEntry(
        name = "Goerli social faucet",
        address = Address("0x8c1e1e5b47980d214965f3bd8ea34c413e120ae4")
)


val allPrePopulationAddresses = listOf(michael, ligi, faucet, goerli_pusher, goerli_simple_faucet, goerli_social_faucet)
