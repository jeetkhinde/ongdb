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
package org.neo4j.cluster.com.message;

import java.util.ArrayList;
import java.util.List;

import org.neo4j.helpers.collection.Iterables;

public class TrackingMessageHolder implements MessageHolder
{
    private final List<Message> messages = new ArrayList<>();

    @Override
    public void offer( Message<? extends MessageType> message )
    {
        messages.add( message );
    }

    public <T extends MessageType> Message<T> single()
    {
        return Iterables.single( messages );
    }

    public <T extends MessageType> Message<T> first()
    {
        return Iterables.first( messages );
    }
}
