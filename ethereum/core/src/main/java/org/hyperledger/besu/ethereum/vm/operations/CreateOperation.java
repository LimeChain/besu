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
package org.hyperledger.besu.ethereum.vm.operations;

import org.hyperledger.besu.ethereum.core.AbstractWorldUpdater;
import org.hyperledger.besu.ethereum.core.Address;
import org.hyperledger.besu.ethereum.core.Gas;
import org.hyperledger.besu.ethereum.vm.GasCalculator;
import org.hyperledger.besu.ethereum.vm.MessageFrame;
import org.hyperledger.besu.ethereum.worldstate.DefaultMutableWorldState;

public class CreateOperation extends AbstractCreateOperation {

  public CreateOperation(final GasCalculator gasCalculator) {
    super(0xF0, "CREATE", 3, 1, false, 1, gasCalculator);
  }

  @Override
  public Gas cost(final MessageFrame frame) {
    return gasCalculator().createOperationGasCost(frame);
  }

  @Override
  protected Address targetContractAddress(final MessageFrame frame) {
    // frame.getWorldState() -> getNewContractAccount(frame.getOriginatorAddress())
    // frame.getOriginatorAddress()

    //    final Account sender = frame.getWorldState().get(frame.getRecipientAddress());
    // Decrement nonce by 1 to normalize the effect of transaction execution
    //    final Address address =
    //        Address.contractAddress(frame.getRecipientAddress(), sender.getNonce() - 1L);

    var updater = frame.getWorldState();

    if (updater instanceof AbstractWorldUpdater) {
      System.out.println("AbstractWorldUpdater");
    } else if (updater instanceof AbstractWorldUpdater.StackedUpdater) {
      System.out.println("AbstractWorldUpdater.StackedUpdater");
    }

    //    final Address address = Address.contractAddress(frame.getRecipientAddress(), 0L);

    //    final Address address =
    //                ((AbstractWorldUpdater) updater)
    //                        .getNewContractAddress(frame.getOriginatorAddress());

    final Address address =
        ((DefaultMutableWorldState.Updater) updater.updater().updater())
            .getNewContractAddress(frame.getOriginatorAddress());

    //        final Address address1 =
    // frame.getWorldState().updater().getNewContractAddress(frame.getOriginatorAddress());

    frame.warmUpAddress(address);
    return address;
  }
}
