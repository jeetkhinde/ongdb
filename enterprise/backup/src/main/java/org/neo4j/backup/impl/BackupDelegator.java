/*
 * Copyright (c) 2018-2020 "Graph Foundation"
 * Graph Foundation, Inc. [https://graphfoundation.org]
 *
 * Copyright (c) 2002-2018 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of ONgDB Enterprise Edition. The included source
 * code can be redistributed and/or modified under the terms of the
 * GNU AFFERO GENERAL PUBLIC LICENSE Version 3
 * (http://www.fsf.org/licensing/licenses/agpl-3.0.html) as found
 * in the associated LICENSE.txt file.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 */
package org.neo4j.backup.impl;

import java.io.IOException;

import org.neo4j.causalclustering.catchup.CatchUpClient;
import org.neo4j.causalclustering.catchup.CatchupAddressProvider;
import org.neo4j.causalclustering.catchup.CatchupResult;
import org.neo4j.causalclustering.catchup.storecopy.RemoteStore;
import org.neo4j.causalclustering.catchup.storecopy.StoreCopyClient;
import org.neo4j.causalclustering.catchup.storecopy.StoreCopyFailedException;
import org.neo4j.causalclustering.catchup.storecopy.StoreIdDownloadFailedException;
import org.neo4j.causalclustering.identity.StoreId;
import org.neo4j.helpers.AdvertisedSocketAddress;
import org.neo4j.io.layout.DatabaseLayout;
import org.neo4j.kernel.lifecycle.LifecycleAdapter;

/**
 * Simplifies the process of performing a backup over the transaction protocol by wrapping all the necessary classes
 * and delegating methods to the correct instances.
 */
class BackupDelegator extends LifecycleAdapter
{
    private final RemoteStore remoteStore;
    private final CatchUpClient catchUpClient;
    private final StoreCopyClient storeCopyClient;

    BackupDelegator( RemoteStore remoteStore, CatchUpClient catchUpClient, StoreCopyClient storeCopyClient )
    {
        this.remoteStore = remoteStore;
        this.catchUpClient = catchUpClient;
        this.storeCopyClient = storeCopyClient;
    }

    void copy( AdvertisedSocketAddress fromAddress, StoreId expectedStoreId, DatabaseLayout databaseLayout ) throws StoreCopyFailedException
    {
        remoteStore.copy( new CatchupAddressProvider.SingleAddressProvider( fromAddress ), expectedStoreId, databaseLayout, true );
    }

    CatchupResult tryCatchingUp( AdvertisedSocketAddress fromAddress, StoreId expectedStoreId, DatabaseLayout databaseLayout ) throws StoreCopyFailedException
    {
        try
        {
            return remoteStore.tryCatchingUp( fromAddress, expectedStoreId, databaseLayout, true, true );
        }
        catch ( IOException e )
        {
            throw new StoreCopyFailedException( e );
        }
    }

    @Override
    public void start()
    {
        catchUpClient.start();
    }

    @Override
    public void stop()
    {
        catchUpClient.stop();
    }

    public StoreId fetchStoreId( AdvertisedSocketAddress fromAddress ) throws StoreIdDownloadFailedException
    {
        return storeCopyClient.fetchStoreId( fromAddress );
    }
}
