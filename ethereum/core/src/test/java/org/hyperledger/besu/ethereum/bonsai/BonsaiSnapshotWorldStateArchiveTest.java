/*
 * Copyright Hyperledger Besu Contributors.
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
package org.hyperledger.besu.ethereum.bonsai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hyperledger.besu.ethereum.bonsai.BonsaiWorldStateKeyValueStorage.WORLD_BLOCK_HASH_KEY;
import static org.hyperledger.besu.ethereum.bonsai.BonsaiWorldStateKeyValueStorage.WORLD_ROOT_HASH_KEY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.hyperledger.besu.ethereum.chain.Blockchain;
import org.hyperledger.besu.ethereum.core.BlockHeader;
import org.hyperledger.besu.ethereum.core.BlockHeaderTestFixture;
import org.hyperledger.besu.ethereum.storage.StorageProvider;
import org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueSegmentIdentifier;
import org.hyperledger.besu.plugin.services.storage.SnappableKeyValueStorage;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BonsaiSnapshotWorldStateArchiveTest {

  final BlockHeaderTestFixture blockBuilder = new BlockHeaderTestFixture();

  @Mock Blockchain blockchain;

  @Mock StorageProvider storageProvider;

  @Mock SnappableKeyValueStorage keyValueStorage;

  BonsaiWorldStateArchive bonsaiWorldStateArchive;

  @Before
  public void setUp() {
    when(storageProvider.getStorageBySegmentIdentifier(any(KeyValueSegmentIdentifier.class)))
        .thenReturn(keyValueStorage);
  }

  @Test
  public void testGetMutableReturnPersistedStateWhenNeeded() {
    final BlockHeader chainHead = blockBuilder.number(0).buildHeader();

    when(keyValueStorage.get(WORLD_ROOT_HASH_KEY))
        .thenReturn(Optional.of(chainHead.getStateRoot().toArrayUnsafe()));
    when(keyValueStorage.get(WORLD_BLOCK_HASH_KEY))
        .thenReturn(Optional.of(chainHead.getHash().toArrayUnsafe()));
    when(keyValueStorage.get(WORLD_ROOT_HASH_KEY))
        .thenReturn(Optional.of(chainHead.getStateRoot().toArrayUnsafe()));
    when(keyValueStorage.get(WORLD_BLOCK_HASH_KEY))
        .thenReturn(Optional.of(chainHead.getHash().toArrayUnsafe()));
    bonsaiWorldStateArchive =
        new BonsaiWorldStateArchive(
            new BonsaiWorldStateKeyValueStorage(storageProvider),
            blockchain,
            Optional.of(1L),
            true);

    assertThat(bonsaiWorldStateArchive.getMutable(null, chainHead.getHash(), true))
        .containsInstanceOf(BonsaiPersistedWorldState.class);
  }

  @Test
  public void testGetMutableReturnEmptyWhenLoadMoreThanLimitLayersBack() {
    bonsaiWorldStateArchive =
        new BonsaiWorldStateArchive(
            new BonsaiWorldStateKeyValueStorage(storageProvider), blockchain, Optional.of(512L));
    final BlockHeader blockHeader = blockBuilder.number(0).buildHeader();
    final BlockHeader chainHead = blockBuilder.number(512).buildHeader();
    when(blockchain.getBlockHeader(eq(blockHeader.getHash()))).thenReturn(Optional.of(blockHeader));
    when(blockchain.getChainHeadHeader()).thenReturn(chainHead);
    assertThat(bonsaiWorldStateArchive.getMutable(null, blockHeader.getHash(), false)).isEmpty();
  }
}
