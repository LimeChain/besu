/*
 * Copyright ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.besu.ethereum.worldstate;

import org.hyperledger.besu.ethereum.core.AbstractWorldUpdater;
import org.hyperledger.besu.ethereum.core.Account;
import org.hyperledger.besu.ethereum.core.AccountStorageEntry;
import org.hyperledger.besu.ethereum.core.Address;
import org.hyperledger.besu.ethereum.core.BlockHeader;
import org.hyperledger.besu.ethereum.core.Hash;
import org.hyperledger.besu.ethereum.core.MutableWorldState;
import org.hyperledger.besu.ethereum.core.UpdateTrackingAccount;
import org.hyperledger.besu.ethereum.core.Wei;
import org.hyperledger.besu.ethereum.core.WorldState;
import org.hyperledger.besu.ethereum.core.WorldUpdater;
import org.hyperledger.besu.ethereum.rlp.RLP;
import org.hyperledger.besu.ethereum.rlp.RLPInput;
import org.hyperledger.besu.ethereum.trie.MerklePatriciaTrie;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt256;

public class DefaultMutableWorldState implements MutableWorldState {

  private final AccountStateStore accountStateStore;
  private final Map<Address, AccountStorageMap> updatedStorageTries = new HashMap<>();
  private final Map<Address, Bytes> updatedAccountCode = new HashMap<>();

  public DefaultMutableWorldState(
      final WorldStateStorage storage, final WorldStatePreimageStorage preimageStorage) {
    this(MerklePatriciaTrie.EMPTY_TRIE_NODE_HASH, storage, preimageStorage);
  }

  // Not Supported
  public DefaultMutableWorldState(
      final Bytes32 rootHash,
      final WorldStateStorage worldStateStorage,
      final WorldStatePreimageStorage preimageStorage) {
    this.accountStateStore = null;
  }

  public DefaultMutableWorldState(final AccountStateStore accountStateStore) {
    this.accountStateStore = accountStateStore;
  }

  public DefaultMutableWorldState(final WorldState worldState) {
    // TODO: this is an abstraction leak (and kind of incorrect in that we reuse the underlying
    // storage), but the reason for this is that the accounts() method is unimplemented below and
    // can't be until NC-754.
    if (!(worldState instanceof DefaultMutableWorldState)) {
      throw new UnsupportedOperationException();
    }
    this.accountStateStore = null;
  }

  private AccountStorageMap newAccountStorageMap(final Address address) {
    return accountStateStore.newStorageMap(address);
  }

  @Override
  public Hash rootHash() {
    return Hash.EMPTY; // Not Supported
  }

  @Override
  public Hash frontierRootHash() {
    return rootHash();
  }

  @Override
  public MutableWorldState copy() {
    return new DefaultMutableWorldState(accountStateStore);
  }

  @Override
  public Account get(final Address address) {
    return accountStateStore.get(address);
  }

  @Override
  public WorldUpdater updater() {
    return new Updater(this);
  }

  @Override
  public Stream<StreamableAccount> streamAccounts(final Bytes32 startKeyHash, final int limit) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(rootHash());
  }

  @Override
  public final boolean equals(final Object other) {
    if (!(other instanceof DefaultMutableWorldState)) {
      return false;
    }

    final DefaultMutableWorldState that = (DefaultMutableWorldState) other;
    return this.rootHash().equals(that.rootHash());
  }

  @Override
  public void persist(final BlockHeader blockHeader) {
    //    final WorldStateStorage.Updater stateUpdater = worldStateStorage.updater();
    //    // Store updated code
    //    // TODO -> Adds/Removes code from the Account state
    //    for (final Bytes code : updatedAccountCode.values()) {
    //      stateUpdater.putCode(null, code);
    //    }
    //    // Commit account storage tries
    //    // TODO -> VirtualMaps where we have to commit?
    //    for (final MerklePatriciaTrie<Bytes32, Bytes> updatedStorage :
    // updatedStorageTries.values()) {
    //      updatedStorage.commit(
    //              (location, hash, value) ->
    //                      stateUpdater.putAccountStorageTrieNode(null, location, hash, value));
    //    }
    //    // Commit account updates
    //    // TODO we will commit the Account changes from here
    //    accountStateTrie.commit(stateUpdater::putAccountStateTrieNode);
    //
    //    // Persist preimages
    //    final WorldStatePreimageStorage.Updater preimageUpdater = preimageStorage.updater();
    //    newStorageKeyPreimages.forEach(preimageUpdater::putStorageTrieKeyPreimage);
    //    newAccountKeyPreimages.forEach(preimageUpdater::putAccountTrieKeyPreimage);
    //
    //    // Clear pending changes that we just flushed
    //    updatedStorageTries.clear();
    //    updatedAccountCode.clear();
    //    newStorageKeyPreimages.clear();
    //
    //    // Push changes to underlying storage
    //    preimageUpdater.commit();
    //    stateUpdater.commit();
  }

  private static UInt256 convertToUInt256(final Bytes value) {
    // TODO: we could probably have an optimized method to decode a single scalar since it's used
    // pretty often.
    final RLPInput in = RLP.input(value);
    return in.readUInt256Scalar();
  }

  // An immutable class that represents an individual account as stored in
  // in the world state's underlying merkle patricia trie.
  public class WorldStateAccount implements Account {

    private final Address address;
    private final long nonce;
    private final Wei balance;

    // Lazily initialized since we don't always access storage.
    private volatile AccountStorageMap storageTrie;

    public WorldStateAccount(final Address address, final long nonce, final Wei balance) {
      this.address = address;
      this.nonce = nonce;
      this.balance = balance;
    }

    private AccountStorageMap storageTrie() {
      final AccountStorageMap updatedTrie = updatedStorageTries.get(address);
      if (updatedTrie != null) {
        storageTrie = updatedTrie;
      }
      if (storageTrie == null) {
        storageTrie = newAccountStorageMap(address);
      }
      return storageTrie;
    }

    @Override
    public Address getAddress() {
      return address;
    }

    @Override
    public Hash getAddressHash() {
      return Hash.EMPTY; // Not supported!
    }

    @Override
    public long getNonce() {
      return nonce;
    }

    @Override
    public Wei getBalance() {
      return balance;
    }

    Hash getStorageRoot() {
      return Hash.EMPTY; // Not supported!
    }

    @Override
    public Bytes getCode() {
      final Bytes updatedCode = updatedAccountCode.get(address);
      if (updatedCode != null) {
        return updatedCode;
      }
      // No code is common, save the KV-store lookup.
      final Hash codeHash = getCodeHash();
      if (codeHash.equals(Hash.EMPTY)) {
        return Bytes.EMPTY;
      }
      return accountStateStore.getCode(address);
    }

    @Override
    public boolean hasCode() {
      return !getCode().isEmpty();
    }

    @Override
    public Hash getCodeHash() {
      return Hash.EMPTY; // Not supported!
    }

    @Override
    public int getVersion() {
      return 1; // Not supported!
    }

    @Override
    public UInt256 getStorageValue(final UInt256 key) {
      return storageTrie()
          .get(key.toBytes())
          .map(DefaultMutableWorldState::convertToUInt256)
          .orElse(UInt256.ZERO);
    }

    @Override
    public UInt256 getOriginalStorageValue(final UInt256 key) {
      return getStorageValue(key);
    }

    @Override
    public NavigableMap<Bytes32, AccountStorageEntry> storageEntriesFrom(
        final Bytes32 startKeyHash, final int limit) {
      throw new UnsupportedOperationException("Stream storage entries not supported");
    }

    @Override
    public String toString() {
      final StringBuilder builder = new StringBuilder();
      builder.append("AccountState").append("{");
      builder.append("address=").append(getAddress()).append(", ");
      builder.append("nonce=").append(getNonce()).append(", ");
      builder.append("balance=").append(getBalance()).append(", ");
      builder.append("storageRoot=").append(getStorageRoot()).append(", ");
      builder.append("codeHash=").append(getCodeHash()).append(", ");
      builder.append("version=").append(getVersion());
      return builder.append("}").toString();
    }
  }

  protected static class Updater
      extends AbstractWorldUpdater<DefaultMutableWorldState, WorldStateAccount> {

    protected Updater(final DefaultMutableWorldState world) {
      super(world);
    }

    @Override
    protected WorldStateAccount getForMutation(final Address address) {
      final DefaultMutableWorldState wrapped = wrappedWorldView();
      Account acc = wrapped.accountStateStore.get(address);
      return acc == null
          ? null
          : wrapped.new WorldStateAccount(acc.getAddress(), acc.getNonce(), acc.getBalance());
    }

    @Override
    public Collection<? extends Account> getTouchedAccounts() {
      return new ArrayList<>(getUpdatedAccounts());
    }

    @Override
    public Collection<Address> getDeletedAccountAddresses() {
      return new ArrayList<>(getDeletedAccounts());
    }

    @Override
    public void revert() {
      getDeletedAccounts().clear();
      getUpdatedAccounts().clear();
    }

    @Override
    public void commit() {
      final DefaultMutableWorldState wrapped = wrappedWorldView();

      for (final Address address : getDeletedAccounts()) {
        wrapped.accountStateStore.remove(address);
      }

      for (final UpdateTrackingAccount<WorldStateAccount> updated : getUpdatedAccounts()) {
        final WorldStateAccount origin = updated.getWrappedAccount();

        // Save the code in storage ...
        if (updated.codeWasUpdated()) {
          wrapped.accountStateStore.putCode(updated.getAddress(), updated.getCode());
        }
        // ...and storage in the account trie first.
        final boolean freshState = origin == null || updated.getStorageWasCleared();
        if (freshState) {
          wrapped.accountStateStore.clearStorage(updated.getAddress());
        }

        final Map<UInt256, UInt256> updatedStorage = updated.getUpdatedStorage();
        if (!updatedStorage.isEmpty()) {
          // Apply any storage updates
          final AccountStorageMap storageTrie =
              freshState ? wrapped.newAccountStorageMap(origin.getAddress()) : origin.storageTrie();
          final TreeSet<Map.Entry<UInt256, UInt256>> entries =
              new TreeSet<>(
                  Comparator.comparing(
                      (Function<Map.Entry<UInt256, UInt256>, UInt256>) Map.Entry::getKey));
          entries.addAll(updatedStorage.entrySet());

          for (final Map.Entry<UInt256, UInt256> entry : entries) {
            final UInt256 value = entry.getValue();
            if (value.isZero()) {
              storageTrie.remove(entry.getKey().toBytes());
            } else {
              // Use UInt256 directly
              storageTrie.put(
                  entry.getKey().toBytes(),
                  RLP.encode(out -> out.writeBytes(entry.getValue().toMinimalBytes())));
            }
          }
          // Commit any state changes
          storageTrie.commit();
        }

        // Lastly, save the new account.
        // TODO we must not allow for arbitrary contract creation. If we do `get` for account and it returns `null` we must halt the execution and revert
        wrapped.accountStateStore.put(
            updated.getAddress(), updated.getNonce(), updated.getBalance());
        // Commit account state changes
        wrapped.accountStateStore.commit();

        // Clear structures
        wrapped.updatedStorageTries.clear();
      }
    }
  }
}
