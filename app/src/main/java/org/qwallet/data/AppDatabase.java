package org.qwallet.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import org.qwallet.data.addressbook.AddressBookDAO;
import org.qwallet.data.addressbook.AddressBookEntry;
import org.qwallet.data.balances.Balance;
import org.qwallet.data.balances.BalanceDAO;
import org.qwallet.data.tokens.Token;
import org.qwallet.data.tokens.TokenDAO;
import org.qwallet.data.transactions.TransactionDAO;
import org.qwallet.data.transactions.TransactionEntity;

@Database(entities = {AddressBookEntry.class, Token.class, Balance.class, TransactionEntity.class}, version = 4)
@TypeConverters({RoomTypeConverters.class})
public abstract class AppDatabase extends RoomDatabase {

    public abstract AddressBookDAO getAddressBook();

    public abstract TokenDAO getTokens();

    public abstract TransactionDAO getTransactions();

    public abstract BalanceDAO getBalances();
}
