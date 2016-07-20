
package com.torodb.packaging;

import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.ProvisionException;
import com.torodb.backend.guice.BackendModule;
import com.torodb.core.BuildProperties;
import com.torodb.core.guice.CoreModule;
import com.torodb.d2r.guice.D2RModule;
import com.torodb.metainfo.guice.MetainfModule;
import com.torodb.mongodb.core.MongodServer;
import com.torodb.mongodb.guice.MongoLayerModule;
import com.torodb.mongodb.repl.ReplCoordinator;
import com.torodb.mongodb.repl.guice.MongoDbReplModule;
import com.torodb.packaging.config.model.Config;
import com.torodb.packaging.config.model.protocol.mongo.Replication;
import com.torodb.packaging.guice.BackendImplementationModule;
import com.torodb.packaging.guice.ConfigModule;
import com.torodb.packaging.guice.PackagingModule;
import com.torodb.torod.TorodServer;
import com.torodb.torod.guice.TorodModule;
import java.time.Clock;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 */
public class ToroDbiServer extends AbstractIdleService {

    private static final Logger LOGGER = LogManager.getLogger(ToroDbServer.class);

    private final BuildProperties buildProperties;
    private final TorodServer torod;
    private final MongodServer mongod;
    private final ReplCoordinator replCoordinator;

    @Inject
    ToroDbiServer(BuildProperties buildProperties, TorodServer torod, MongodServer mongod, ReplCoordinator replCoordinator) {
        this.buildProperties = buildProperties;
        this.torod = torod;
        this.mongod = mongod;
        this.replCoordinator = replCoordinator;
    }

    public static ToroDbiServer create(Config config, Clock clock) throws ProvisionException {
        Injector injector = createInjector(config, clock);
        return injector.getInstance(ToroDbiServer.class);
    }

    @SuppressWarnings("deprecation")
    public static Injector createInjector(Config config, Clock clock) {
        List<Replication> replications = config.getProtocol().getMongo().getReplication();
        if (replications == null) {
            throw new IllegalArgumentException("Replication section (protocol.mongo.replication) must be set");
        }
        if (replications.size() != 1) {
            throw new IllegalArgumentException("Exactly one protocol.mongo.replication must be set");
        }
        String syncSourceString = replications.stream().findAny().get().getSyncSource();
        HostAndPort syncSource = HostAndPort.fromString(syncSourceString)
                .withDefaultPort(27017);

        Injector injector = Guice.createInjector(
                new ConfigModule(config),
                new PackagingModule(clock),
                new CoreModule(),
                new BackendImplementationModule(config),
                new BackendModule(),
                new MetainfModule(),
                new D2RModule(),
                new TorodModule(),
                new MongoLayerModule(),
                new MongoDbReplModule(syncSource)
        );
        return injector;
    }

    @Override
    protected void startUp() throws Exception {
        LOGGER.info("Starting up ToroDB v" +  buildProperties.getFullVersion());

        torod.startAsync();
        mongod.startAsync();

        LOGGER.debug("Waiting for Mongod to be running");
        mongod.awaitRunning();

        LOGGER.debug("Waiting for Torod to be running");
        torod.awaitRunning();
        LOGGER.debug("Waiting for Replication to be running");
        replCoordinator.startAsync();
        replCoordinator.awaitRunning();

        LOGGER.debug("ToroDbiServer ready to run");
    }

    @Override
    protected void shutDown() throws Exception {
        LOGGER.info("Shutting down ToroDB");

        replCoordinator.stopAsync();
        replCoordinator.awaitTerminated();

        mongod.stopAsync();
        mongod.awaitTerminated();

        torod.stopAsync();
        torod.awaitTerminated();

        LOGGER.debug("ToroDBServer shutdown complete");
    }


}
