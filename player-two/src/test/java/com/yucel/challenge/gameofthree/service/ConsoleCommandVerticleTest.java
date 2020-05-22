package com.yucel.challenge.gameofthree.service;

import com.yucel.challenge.gameofthree.PlayerTwoMainVerticle;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(VertxExtension.class)
public class ConsoleCommandVerticleTest {

  @BeforeEach
  void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
    vertx.deployVerticle(new PlayerTwoMainVerticle(), testContext.succeeding(id -> testContext.completeNow()));
  }

  @Test
  void givenNonInitializedPlayer_whenRequestStatus_thenGetNullMoveObject(Vertx vertx, VertxTestContext testContext) throws Throwable{
    vertx.eventBus().request("com.yucel.game.status.player.two", null, messageAsyncResult -> {
      // move object
      assertNull(messageAsyncResult.result());
      testContext.completeNow();
    });
  }

  @AfterEach
  void close(Vertx vertx, VertxTestContext testContext){
    vertx.close(voidAsyncResult -> testContext.completeNow());
  }
}
