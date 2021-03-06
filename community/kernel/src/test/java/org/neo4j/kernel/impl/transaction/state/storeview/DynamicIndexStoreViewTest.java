/*
 * Copyright (c) 2018-2020 "Graph Foundation"
 * Graph Foundation, Inc. [https://graphfoundation.org]
 *
 * Copyright (c) 2002-2020 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of ONgDB.
 *
 * ONgDB is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel.impl.transaction.state.storeview;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.function.IntPredicate;

import org.neo4j.collection.PrimitiveLongResourceCollections;
import org.neo4j.collection.PrimitiveLongResourceIterator;
import org.neo4j.helpers.collection.Visitor;
import org.neo4j.io.pagecache.PageCursor;
import org.neo4j.kernel.api.labelscan.AllEntriesLabelScanReader;
import org.neo4j.kernel.api.labelscan.LabelScanStore;
import org.neo4j.kernel.api.labelscan.NodeLabelUpdate;
import org.neo4j.kernel.impl.api.index.EntityUpdates;
import org.neo4j.kernel.impl.api.index.StoreScan;
import org.neo4j.kernel.impl.locking.LockService;
import org.neo4j.kernel.impl.store.NeoStores;
import org.neo4j.kernel.impl.store.NodeStore;
import org.neo4j.kernel.impl.store.counts.CountsTracker;
import org.neo4j.kernel.impl.store.record.NodeRecord;
import org.neo4j.kernel.impl.store.record.RecordLoad;
import org.neo4j.logging.NullLogProvider;
import org.neo4j.register.Register;
import org.neo4j.register.Registers;
import org.neo4j.storageengine.api.schema.LabelScanReader;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

public class DynamicIndexStoreViewTest
{
    private final LabelScanStore labelScanStore = mock( LabelScanStore.class );
    private final NeoStores neoStores = mock( NeoStores.class );
    private final NodeStore nodeStore = mock( NodeStore.class );
    private final CountsTracker countStore = mock( CountsTracker.class );
    private final Visitor<EntityUpdates,Exception> propertyUpdateVisitor = mock( Visitor.class );
    private final Visitor<NodeLabelUpdate,Exception> labelUpdateVisitor = mock( Visitor.class );
    private final IntPredicate propertyKeyIdFilter = mock( IntPredicate.class );
    private final AllEntriesLabelScanReader nodeLabelRanges = mock( AllEntriesLabelScanReader.class );

    @Before
    public void setUp()
    {
        NodeRecord nodeRecord = getNodeRecord();
        when( labelScanStore.allNodeLabelRanges()).thenReturn( nodeLabelRanges );
        when( neoStores.getCounts() ).thenReturn( countStore );
        when( neoStores.getNodeStore() ).thenReturn( nodeStore );
        when( nodeStore.newRecord() ).thenReturn( nodeRecord );
        doAnswer( invocation ->
        {
            NodeRecord record = invocation.getArgument( 1 );
            record.initialize( true, 1L, false, 1L, 0L );
            record.setId( invocation.getArgument( 0 ) );
            return null;
        } ).when( nodeStore ).getRecordByCursor( anyLong(), any( NodeRecord.class ), any( RecordLoad.class ), any( PageCursor.class ) );
        doAnswer( invocation ->
        {
            NodeRecord record = invocation.getArgument( 0 );
            record.initialize( true, 1L, false, 1L, 0L );
            record.setId( record.getId() + 1 );
            return null;
        } ).when( nodeStore ).nextRecordByCursor( any( NodeRecord.class ), any( RecordLoad.class ), any( PageCursor.class ) );
    }

    @Test
    public void visitOnlyLabeledNodes() throws Exception
    {
        LabelScanReader labelScanReader = mock( LabelScanReader.class );
        when( labelScanStore.newReader() ).thenReturn( labelScanReader );
        when( nodeLabelRanges.maxCount() ).thenReturn( 1L );

        PrimitiveLongResourceIterator labeledNodesIterator = PrimitiveLongResourceCollections.iterator( null, 1, 2, 3, 4, 5, 6, 7, 8 );
        when( nodeStore.getHighestPossibleIdInUse() ).thenReturn( 200L );
        when( nodeStore.getHighId() ).thenReturn( 20L );
        when( labelScanReader.nodesWithAnyOfLabels( new int[] {2, 6} ) ).thenReturn( labeledNodesIterator );
        when( nodeStore.openPageCursorForReading( anyLong() ) ).thenReturn( mock( PageCursor.class ) );

        mockLabelNodeCount( countStore, 2 );
        mockLabelNodeCount( countStore, 6 );

        DynamicIndexStoreView storeView = dynamicIndexStoreView();

        StoreScan<Exception> storeScan = storeView
                .visitNodes( new int[]{2, 6}, propertyKeyIdFilter, propertyUpdateVisitor, labelUpdateVisitor, false );

        storeScan.run();

        Mockito.verify( nodeStore, times( 8 ) )
                .getRecordByCursor( anyLong(), any( NodeRecord.class ), any( RecordLoad.class ), any( PageCursor.class ) );
    }

    @Test
    public void shouldBeAbleToForceStoreScan() throws Exception
    {
        when( labelScanStore.newReader() ).thenThrow( new RuntimeException( "Should not be used" ) );

        when( nodeStore.getHighestPossibleIdInUse() ).thenReturn( 200L );
        when( nodeStore.getHighId() ).thenReturn( 20L );
        when( nodeStore.openPageCursorForReading( anyLong() ) ).thenReturn( mock( PageCursor.class ) );

        mockLabelNodeCount( countStore, 2 );
        mockLabelNodeCount( countStore, 6 );

        DynamicIndexStoreView storeView = dynamicIndexStoreView();

        StoreScan<Exception> storeScan = storeView
                .visitNodes( new int[]{2, 6}, propertyKeyIdFilter, propertyUpdateVisitor, labelUpdateVisitor, true );

        storeScan.run();

        Mockito.verify( nodeStore, times( 1 ) )
                .getRecordByCursor( anyLong(), any( NodeRecord.class ), any( RecordLoad.class ), any( PageCursor.class ) );
        Mockito.verify( nodeStore, times( 200 ) )
                .nextRecordByCursor( any( NodeRecord.class ), any( RecordLoad.class ), any( PageCursor.class ) );
    }

    private DynamicIndexStoreView dynamicIndexStoreView()
    {
        LockService locks = LockService.NO_LOCK_SERVICE;
        return new DynamicIndexStoreView( new NeoStoreIndexStoreView( locks, neoStores ), labelScanStore,
                locks, neoStores, NullLogProvider.getInstance() );
    }

    private NodeRecord getNodeRecord()
    {
        NodeRecord nodeRecord = new NodeRecord( 0L );
        nodeRecord.initialize( true, 1L, false, 1L, 0L );
        return nodeRecord;
    }

    private void mockLabelNodeCount( CountsTracker countStore, int labelId )
    {
        Register.DoubleLongRegister register = Registers.newDoubleLongRegister( labelId, labelId );
        when( countStore.nodeCount( eq( labelId ), any( Register.DoubleLongRegister.class ) ) ).thenReturn( register );
    }

}
