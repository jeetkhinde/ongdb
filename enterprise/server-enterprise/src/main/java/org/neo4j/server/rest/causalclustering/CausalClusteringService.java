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
package org.neo4j.server.rest.causalclustering;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.server.rest.management.AdvertisableService;
import org.neo4j.server.rest.repr.OutputFormat;

@Path( CausalClusteringService.BASE_PATH )
public class CausalClusteringService implements AdvertisableService
{
    static final String BASE_PATH = "server/causalclustering/";

    static final String AVAILABLE = "available";
    static final String WRITABLE = "writable";
    static final String READ_ONLY = "read-only";
    static final String DESCRIPTION = "status";

    private final CausalClusteringStatus status;

    public CausalClusteringService( @Context OutputFormat output, @Context GraphDatabaseService db )
    {
        this.status = CausalClusteringStatusFactory.build( output, db );
    }

    @GET
    public Response discover()
    {
        return status.discover();
    }

    @GET
    @Path( WRITABLE )
    public Response isWritable()
    {
        return status.writable();
    }

    @GET
    @Path( READ_ONLY )
    public Response isReadOnly()
    {
        return status.readonly();
    }

    @GET
    @Path( AVAILABLE )
    public Response isAvailable()
    {
        return status.available();
    }

    @GET
    @Path( DESCRIPTION )
    public Response status()
    {
        return status.description();
    }

    @Override
    public String getName()
    {
        return "causalclustering";
    }

    @Override
    public String getServerPath()
    {
        return BASE_PATH;
    }
}
