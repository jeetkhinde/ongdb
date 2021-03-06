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
package org.neo4j.unsafe.impl.batchimport.staging;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

import org.neo4j.helpers.collection.Pair;
import org.neo4j.helpers.collection.PrefetchingIterator;
import org.neo4j.unsafe.impl.batchimport.Configuration;
import org.neo4j.unsafe.impl.batchimport.stats.Key;
import org.neo4j.unsafe.impl.batchimport.stats.Stat;

import static org.neo4j.helpers.Exceptions.throwIfUnchecked;

/**
 * Default implementation of {@link StageControl}
 */
public class StageExecution implements StageControl, AutoCloseable
{
    private final String stageName;
    private final String part;
    private final Configuration config;
    private final Collection<Step<?>> pipeline;
    private final int orderingGuarantees;
    private volatile Throwable panic;
    private final boolean shouldRecycle;
    private final ConcurrentLinkedQueue<Object> recycled;

    public StageExecution( String stageName, String part, Configuration config, Collection<Step<?>> pipeline,
            int orderingGuarantees )
    {
        this.stageName = stageName;
        this.part = part;
        this.config = config;
        this.pipeline = pipeline;
        this.orderingGuarantees = orderingGuarantees;
        this.shouldRecycle = (orderingGuarantees & Step.RECYCLE_BATCHES) != 0;
        this.recycled = shouldRecycle ? new ConcurrentLinkedQueue<>() : null;
    }

    public boolean stillExecuting()
    {
        for ( Step<?> step : pipeline )
        {
            if ( !step.isCompleted() )
            {
                return true;
            }
        }
        return false;
    }

    public void start()
    {
        for ( Step<?> step : pipeline )
        {
            step.start( orderingGuarantees );
        }
    }

    public String getStageName()
    {
        return stageName;
    }

    public String name()
    {
        return stageName + (part != null ? part : "");
    }

    public Configuration getConfig()
    {
        return config;
    }

    public Iterable<Step<?>> steps()
    {
        return pipeline;
    }

    /**
     * @param stat statistics {@link Key}.
     * @param trueForAscending {@code true} for ordering by ascending, otherwise descending.
     * @return the steps ordered by the {@link Stat#asLong() long value representation} of the given
     * {@code stat} accompanied a factor by how it compares to the next value, where a value close to
     * {@code 1.0} signals them being close to equal, and a value of for example {@code 0.5} signals that
     * the value of the current step is half that of the next step.
     */
    public Iterable<Pair<Step<?>,Float>> stepsOrderedBy( final Key stat, final boolean trueForAscending )
    {
        final List<Step<?>> steps = new ArrayList<>( pipeline );
        steps.sort( ( o1, o2 ) -> {
            Long stat1 = o1.stats().stat( stat ).asLong();
            Long stat2 = o2.stats().stat( stat ).asLong();
            return trueForAscending ? stat1.compareTo( stat2 ) : stat2.compareTo( stat1 );
        } );

        return () -> new PrefetchingIterator<Pair<Step<?>,Float>>()
        {
            private final Iterator<Step<?>> source = steps.iterator();
            private Step<?> next = source.hasNext() ? source.next() : null;

            @Override
            protected Pair<Step<?>,Float> fetchNextOrNull()
            {
                if ( next == null )
                {
                    return null;
                }

                Step<?> current = next;
                next = source.hasNext() ? source.next() : null;
                float factor = next != null
                        ? (float) stat( current, stat ) / (float) stat( next, stat )
                        : 1.0f;
                return Pair.of( current, factor );
            }

            private long stat( Step<?> step, Key stat12 )
            {
                return step.stats().stat( stat12 ).asLong();
            }
        };
    }

    public int size()
    {
        return pipeline.size();
    }

    @Override
    public synchronized void panic( Throwable cause )
    {
        if ( panic == null )
        {
            panic = cause;
            for ( Step<?> step : pipeline )
            {
                step.receivePanic( cause );
                step.endOfUpstream();
            }
        }
        else
        {
            if ( !panic.equals( cause ) )
            {
                panic.addSuppressed( cause );
            }
        }
    }

    @Override
    public void assertHealthy()
    {
        if ( panic != null )
        {
            throwIfUnchecked( panic );
            throw new RuntimeException( panic );
        }
    }

    @Override
    public String toString()
    {
        return getClass().getSimpleName() + "[" + name() + "]";
    }

    @Override
    public void recycle( Object batch )
    {
        if ( shouldRecycle )
        {
            recycled.offer( batch );
        }
    }

    @Override
    public <T> T reuse( Supplier<T> fallback )
    {
        if ( shouldRecycle )
        {
            @SuppressWarnings( "unchecked" )
            T result = (T) recycled.poll();
            if ( result != null )
            {
                return result;
            }
        }

        return fallback.get();
    }

    @Override
    public void close()
    {
        if ( shouldRecycle )
        {
            recycled.clear();
        }
    }
}
