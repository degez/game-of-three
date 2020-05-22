package com.yucel.challenge.gameofthree.service;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.yucel.challenge.gameofthree.config.ServiceBinder;
import com.yucel.challenge.gameofthree.model.Move;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class GameMoveVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(GameMoveVerticle.class);
  public static final String OPPONENT_MOVE_HANDLER_KEY = "opponent.move.handler.address";
  public static final String MY_MOVE_HANDLER_ADDRESS_KEY = "my.move.handler.address";
  public static final String MY_MANUAL_MOVE_HANDLER_ADDRESS = "my.manual.move.handler.address";
  public static final String GAME_MODE_HANDLER_ADDRESS_KEY = "game.mode.handler.address";
  private ConfigRetriever retriever;

  @Inject
  private MoveCalculator moveCalculator;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    Guice.createInjector(new ServiceBinder()).injectMembers(this);

    retriever = ConfigRetriever.create(vertx);
    retriever.getConfig(jsonObjectAsyncResult -> {
      jsonObjectAsyncResult.result().stream().forEach(LOGGER::debug);

      String myMoveHandlerAddress = jsonObjectAsyncResult.result().getString(MY_MOVE_HANDLER_ADDRESS_KEY);
      String myManualMoveHandlerAddress = jsonObjectAsyncResult.result().getString(MY_MANUAL_MOVE_HANDLER_ADDRESS);

      vertx.eventBus().consumer(myMoveHandlerAddress, this::handleOpponentsMove);
      vertx.eventBus().consumer(myManualMoveHandlerAddress, this::handleManualAnswer);
      startPromise.complete();
    });
  }

  private void handleOpponentsMove(Message<Object> message) {
    Move move = extractMoveFromMessage(message, "received opponent move: {}");

    String ourName = move.getOpponentName();
    String opponentName = move.getPlayerName();

    move.setOpponentsMove(move.getMove());
    move.setOpponentsResult(move.getResult());
    move.setOpponentName(opponentName);
    move.setPlayerName(ourName);


    if (move.getOpponentsResult() > 1) {
      String gameModeHandlerAddress = retriever.getCachedConfig().getString(GAME_MODE_HANDLER_ADDRESS_KEY);

      vertx.eventBus().request(gameModeHandlerAddress, JsonObject.mapFrom(move), handleGameModeRequest(move));

    } else if (move.getOpponentsResult() == 1) {
      System.out.println("opponent won :(");
      System.out.println("type reset to start again, you can change game_mode too");
    }

    message.reply(Boolean.TRUE);
  }

  private Handler<AsyncResult<Message<Object>>> handleGameModeRequest(Move move) {
    return messageAsyncResult -> {
      if (messageAsyncResult.succeeded()) {
        LOGGER.debug("success game mode request for the opponent move: {}", JsonObject.mapFrom(move));
        String replyForGameMode = messageAsyncResult.result().body().toString();
        boolean gameModeAuto = Boolean.parseBoolean(replyForGameMode);

        if (gameModeAuto) {
          Move moveResponse = moveCalculator.calculateNextMove(move);

          System.out.println("* player " + moveResponse.getOpponentName()
            + " moved: "
            + moveResponse.getOpponentsMove()
            + " and their resulting number is: "
            + moveResponse.getOpponentsResult());
          sendAutoMoveEventToOpponent(moveResponse);
          if (moveResponse.getResult() == 1) {
            printWonMessages();
          }
        } else {
          // let console verticle handle
        }

      } else {
        LOGGER.error("message sending failed. {}", messageAsyncResult.cause());
      }
    };
  }

  private void printWonMessages() {
    System.out.println("you won!");
    System.out.println("type reset to start again, you can change game_mode too");
  }

  private void sendAutoMoveEventToOpponent(Move moveResponse) {
    String opponentMoveHandlerAddress = retriever.getCachedConfig().getString(OPPONENT_MOVE_HANDLER_KEY);
    LOGGER.debug("will send auto move event: " + moveResponse);
    vertx.eventBus().request(opponentMoveHandlerAddress, JsonObject.mapFrom(moveResponse), handleReplyToOpponent(moveResponse));
  }

  private Handler<AsyncResult<Message<Object>>> handleReplyToOpponent(Move moveResponse) {
    return messageAsyncResultAi -> {
      if (messageAsyncResultAi.succeeded()) {
        LOGGER.debug("success AI reply: " + JsonObject.mapFrom(moveResponse));
        System.out.println("your AI played move: " + moveResponse.getMove() + " resulting number: " + moveResponse.getResult());

      } else {
        LOGGER.error("message sending failed. {}", messageAsyncResultAi.cause());

      }
    };
  }

  private void handleManualAnswer(Message<Object> message) {

    Move move = extractMoveFromMessage(message, "received manual move: {}");
    int sum = move.getOpponentsResult() + move.getMove();

    if (sum % 3 != 0) {
      System.out.println("the number you enter and given number's sum needs to be divided by 3 without remainder");
      System.out.println("given number: " + move.getOpponentsResult());
    } else {

      int result = sum / 3;

      if (move.getOpponentsResult() == 1) {
        System.out.println("you lost :(");
        System.out.println("type reset to start again, you can change game_mode too");

      } else if (result == 1) {
        printWonMessages();
      }
      move.setResult(result);
      String opponentMoveHandlerAddress = retriever.getCachedConfig().getString(OPPONENT_MOVE_HANDLER_KEY);

      vertx.eventBus().send(opponentMoveHandlerAddress, JsonObject.mapFrom(move));
    }
  }

  private Move extractMoveFromMessage(Message<Object> message, String s) {
    String body = message.body().toString();
    LOGGER.info(s, body);
    LOGGER.debug("message address: " + message.address());

    JsonObject bodyJson = (JsonObject) message.body();
    return bodyJson.mapTo(Move.class);
  }

}
