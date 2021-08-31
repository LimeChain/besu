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
 *
 */

package org.hyperledger.besu.ethereum.worldstate;

import org.hyperledger.besu.ethereum.core.Account;
import org.hyperledger.besu.ethereum.core.Address;
import org.hyperledger.besu.ethereum.core.Wei;

import org.apache.tuweni.bytes.Bytes;

/**
 * The state interface used by the EVM to access/modify Hedera Services resources such as {@link
 * Account} and {@link AccountStorageMap}
 */
public interface AccountStateStore {

  /**
   * Gets mutable account from State. EVM is executing this call everytime it needs to access a
   * contract/address, f.e getting recipient address multiple times during 1 contract executions
   *
   * @param address Address for which to get the Account
   * @return Account. Null if not present
   */
  Account get(Address address);

  /**
   * Returns a new instance of {@link AccountStorageMap} for the specified account The account
   * storage map is accessed during Contract execution for reading contract storage ({@link
   * org.apache.tuweni.units.bigints.UInt256} key/value pairs and once the transaction executes, the
   * buffered changes in the {@link org.hyperledger.besu.ethereum.core.AbstractWorldUpdater} are
   * committed to the {@link DefaultMutableWorldState} where the modified {@link AccountStorageMap}
   * are updated
   *
   * @param address the address to get storage map for
   * @return AccountStorageMap of the account
   */
  AccountStorageMap newStorageMap(Address address);

  /**
   * Provisionally updates the provided properties of a given {@link Address}. The specified account
   * might not exist in the Hedera Ledger. In that case the transaction must revert
   *
   * @param address the address to update
   * @param nonce the new nonce of the account
   * @param balance the new balance of the account
   */
  void put(Address address, long nonce, Wei balance);

  /**
   * Provisionally updates the code for a given {@link Account}
   *
   * @param address the address to update
   * @param code the new code for the account
   */
  void putCode(Address address, Bytes code);

  /**
   * Retrieves the smart contract code for a given {@link Address}
   *
   * @param address the address to get the code for
   * @return Bytes of the contract code. Must return `Bytes.EMPTY` if the address does not have
   *     contract code
   */
  Bytes getCode(Address address);

  /**
   * Provisionally removes a given address from state. Clears the contract code and storage of the
   * account as-well
   *
   * @param address the address to be removed.
   */
  void remove(Address address);

  /**
   * Provisionally clears the storage for a given address in state. Called when new contract is
   * being deployed or already existing contract is being destroyed
   *
   * @param address the address to get their storage cleared
   */
  void clearStorage(Address address);

  /**
   * Commits the performed changes into the State. All write operations must be provisional and
   * committed into state with the commit method
   */
  void commit();

  //  Account getNewContractId(Supplier<SequenceNumber> seqNo, Account senderAccount) {

  Address getNewContractAccount(Address sponsor);
}
