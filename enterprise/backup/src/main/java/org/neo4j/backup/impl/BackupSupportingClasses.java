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

import java.util.Collection;

import org.neo4j.io.pagecache.PageCache;
import org.neo4j.kernel.impl.api.CloseableResourceManager;

class BackupSupportingClasses implements AutoCloseable
{
    // Strategies
    private final BackupDelegator backupDelegator;
    private final BackupProtocolService backupProtocolService;
    private final CloseableResourceManager closeableResourceManager;

    // Dependency Helpers
    private final PageCache pageCache;

    BackupSupportingClasses( BackupDelegator backupDelegator, BackupProtocolService backupProtocolService, PageCache pageCache,
            Collection<AutoCloseable> closeables )
    {
        this.backupDelegator = backupDelegator;
        this.backupProtocolService = backupProtocolService;
        this.pageCache = pageCache;
        this.closeableResourceManager = new CloseableResourceManager();
        closeables.forEach( closeableResourceManager::registerCloseableResource );
    }

    public BackupDelegator getBackupDelegator()
    {
        return backupDelegator;
    }

    public BackupProtocolService getBackupProtocolService()
    {
        return backupProtocolService;
    }

    public PageCache getPageCache()
    {
        return pageCache;
    }

    @Override
    public void close()
    {
        closeableResourceManager.closeAllCloseableResources();
    }
}
