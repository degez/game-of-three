package com.yucel.challenge.gameofthree;

import com.yucel.challenge.gameofthree.service.ConsoleCommandVerticle;
import com.yucel.challenge.gameofthree.service.GameMoveVerticle;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;


/**
 * Main Verticle of the project. Deploys GameMoveVerticle and ConsoleCommandVerticle.
 * Creates an instance of Vertx in a clustered manner, so that other Vertx instances can join
 * and communicate each other using the EventBus.
 *
 */
public class PlayerTwoMainVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(PlayerTwoMainVerticle.class);

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    ClusterManager mgr = new HazelcastClusterManager();

    VertxOptions options = new VertxOptions().setClusterManager(mgr);
    Vertx.clusteredVertx(options, vertxAsyncResult -> {
      if (vertxAsyncResult.succeeded()) {
        vertx = vertxAsyncResult.result();

        vertx.deployVerticle(GameMoveVerticle.class.getName());
        vertx.deployVerticle(ConsoleCommandVerticle.class.getName());

        LOGGER.info("all verticles deployed");
        startPromise.complete();
      } else {
        LOGGER.error("initialization failed" + vertxAsyncResult.cause().getMessage());
        startPromise.fail("init failed");
      }
    });

  }
}
