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

public interface AccountStateStore {

  /**
   * Gets mutable account from State
   *
   * @param address Address for which to get the Account
   * @return Account. Null if not present
   */
  Account get(Address address);

  /**
   * Returns a new instance of AccountStorageMap for the specified account
   *
   * @param address the address to get storage map for
   * @return AccountStorageMap of the account
   */
  AccountStorageMap newStorageMap(Address address);

  /**
   * Updates the provided properties for a given Account
   *
   * @param address the address to update
   * @param nonce the new nonce of the account
   * @param balance the new balance of the account
   */
  void put(Address address, long nonce, Wei balance);

  // TODO how we will store Contract bytecode? -> for now we are using FcBlobStore(?)
  // MerkleOptionalBlob
  /**
   * Updates the Code for a given Account
   *
   * @param address the address to update
   * @param code the new code for the account
   */
  void putCode(Address address, Bytes code);

  /**
   * Retrieves the Smart contract code for a given Address
   *
   * @param address the address to get the code for
   * @return Bytes of the contract code
   */
  Bytes getCode(Address address);

  /**
   * Removes a given address from state. Must clear the contract code and storage of the account
   * as-well!
   *
   * @param address the address to be removed.
   */
  void remove(Address address);

  /**
   * Clears the storage for a given address in state.
   *
   * @param address the address to get their storage cleared
   */
  void clearStorage(Address address);

  /**
   * Commits the performed changes into the State. All operations must be provisional and applied on
   * commit
   */
  void commit();
}
