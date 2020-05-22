package com.yucel.challenge.gameofthree.service;

import com.yucel.challenge.gameofthree.model.GameType;
import com.yucel.challenge.gameofthree.model.Move;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.*;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.HashSet;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import static com.yucel.challenge.gameofthree.service.GameMoveVerticle.*;

public class ConsoleCommandVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleCommandVerticle.class);
  public static final String GAME_STATUS_HANDLER_ADDRESS_KEY = "game.status.handler.address";
  public static final String GAME_STATUS_HANDLER_OPPONENT_ADDRESS_KEY = "game.status.handler.opponent.address";
  public static final String EXECUTOR_NAME = "console-player-one";
  public static final int THREAD_COUNT = 1;
  public static final int DURATION = 1;

  private ConfigRetriever retriever;

  Boolean isGameAuto = null;
  Move currentMove;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {


    retriever = ConfigRetriever.create(vertx);
    retriever.getConfig(jsonObjectAsyncResult -> {
      LOGGER.info("config loaded");
      String gameModeHandlerAddressKey = jsonObjectAsyncResult.result().getString(GAME_MODE_HANDLER_ADDRESS_KEY);
      String gameStatusHandlerAddressKey = jsonObjectAsyncResult.result().getString(GAME_STATUS_HANDLER_ADDRESS_KEY);
      vertx.eventBus().consumer(gameModeHandlerAddressKey, this::handleAndProvideGameMode);
      vertx.eventBus().consumer(gameStatusHandlerAddressKey, this::handleGameStatus);
      startPromise.complete();
    });
    WorkerExecutor workerExecutor = vertx.createSharedWorkerExecutor(EXECUTOR_NAME, THREAD_COUNT, DURATION, TimeUnit.DAYS);
    workerExecutor.executeBlocking(this::handleCommands, asyncResult -> {
      LOGGER.info("console process ended");

    });

  }

  private void handleCommands(Promise<Object> promise) {

    HashSet<String> commandSet = initCommandSet();

    Move move = null;

    Scanner scanner = new Scanner(System.in);
    String input;

    printCommands();
    System.out.println("start by setting player name -> player:yourName");

    while (!"quit".equals(input = scanner.nextLine())) {
      try {
        // filter commands
        if (commandSet.stream().anyMatch(input::startsWith)) {

          if (input.startsWith("player")) {
            move = new Move(input.replace("player:", ""));
            currentMove = new Move(input.replace("player:", ""));
          } else if (input.startsWith("reset")) {
            isGameAuto = null;
            move = null;
            printCommands();
          } else if (input.startsWith("help")) {
            printCommands();
          }

          if (move == null) {

            System.out.println("please enter player name (player:playerName)");

          } else if (input.startsWith("game_mode")) {
            processGameModeInput(input);
          } else if (isGameAuto == null) {
            System.out.println("please choose game mode (game_mode:automatic-game_mode:manual)");
          } else {

            if (isGameAuto) {

              processAutoMove(move);

            } else if (input.startsWith("move:")) {
              processManualMove(input);
            }
          }

        } else {
          System.out.println("unknown command please use one below:");
          printCommands();
        }
      } catch (NumberFormatException e) {
        LOGGER.error(e.getMessage(), e);
        System.out.println("expecting number");
      }

    }
    vertx.close();
    System.exit(0);

  }

  private void processAutoMove(Move move) {
    System.out.println("auto move starts with: " + move.getResult());

    Move finalMove = move;

    String gameStatusOpponentHandlerAddress = retriever.getCachedConfig().getString(GAME_STATUS_HANDLER_OPPONENT_ADDRESS_KEY);

    vertx.eventBus().request(gameStatusOpponentHandlerAddress, null, checkStatusAndMoveHandler(finalMove));
  }

  private void processManualMove(String input) {
    String moveStr = input.replace("move:", "");
    Integer manualMove = Integer.valueOf(moveStr);
    if (manualMove < -1 || manualMove > 1) {
      System.out.println(" * move must be one of these: 0, -1, 1");
    } else {

      Move finalMove = currentMove;
      finalMove.setMove(manualMove);
      String gameStatusOpponentHandlerAddress = retriever.getCachedConfig().getString(GAME_STATUS_HANDLER_OPPONENT_ADDRESS_KEY);

      vertx.eventBus().request(gameStatusOpponentHandlerAddress, null, checkStatusAndManualMoveHandler(manualMove, finalMove));
    }
  }

  private void processGameModeInput(String input) {
    String extractedGameMode = input.replace("game_mode:", "");

    if (GameType.AUTOMATIC.getType().equals(extractedGameMode)) {
      isGameAuto = Boolean.TRUE;
      System.out.println("game mode set to automatic");
      System.out.println("type begin to kick it");
    } else if (GameType.MANUAL.getType().equals(extractedGameMode)) {
      isGameAuto = Boolean.FALSE;
      System.out.println("game mode set to manual");
      System.out.println("starting number: " + currentMove.getResult());
    } else {
      System.out.println("automatic or manual can be only game modes");
    }
  }

  private Handler<AsyncResult<Message<Object>>> checkStatusAndManualMoveHandler(Integer manualMove, Move finalMove) {
    return messageAsyncResult -> {
      if (messageAsyncResult.succeeded()) {
        Object body = messageAsyncResult.result().body();
        if (body == null) {
          System.out.println("* opponent is not ready, please make your move again");
        } else {
          // kick off the game by sending message
          JsonObject bodyJson = (JsonObject) body;
          Move opponent = bodyJson.mapTo(Move.class);
          finalMove.setOpponentName(opponent.getPlayerName());
          finalMove.setMove(manualMove);

          String myManualMoveHandlerAddress = retriever.getCachedConfig().getString(MY_MANUAL_MOVE_HANDLER_ADDRESS);
          vertx.eventBus().send(myManualMoveHandlerAddress, JsonObject.mapFrom(finalMove));
        }
      } else {
        System.out.println("no opponent application available, please start the other instance - " + messageAsyncResult.cause());
      }
    };
  }

  private Handler<AsyncResult<Message<Object>>> checkStatusAndMoveHandler(Move finalMove) {
    return messageAsyncResult -> {
      if (messageAsyncResult.succeeded()) {
        Object body = messageAsyncResult.result().body();
        if (body == null) {
          System.out.println("* opponent is not ready, please make your move again");
        } else {
          // kick off the game by sending message
          JsonObject bodyJson = (JsonObject) body;
          Move opponent = bodyJson.mapTo(Move.class);
          finalMove.setOpponentName(opponent.getPlayerName());

          String opponentMoveHandlerAddress = retriever.getCachedConfig().getString(OPPONENT_MOVE_HANDLER_KEY);
          vertx.eventBus().send(opponentMoveHandlerAddress, JsonObject.mapFrom(finalMove));
          System.out.println(finalMove.getPlayerName() + " moved " + finalMove.getResult() + " - against " + opponent.getPlayerName());

        }
      } else {
        System.out.println("no opponent application available, please start the other instance - " + messageAsyncResult.cause());
      }
    };
  }

  private void handleAndProvideGameMode(Message<Object> message) {
    LOGGER.debug("received is game auto request");

    if (isGameAuto) {
      message.reply(Boolean.TRUE);
    } else {
      JsonObject bodyJson = (JsonObject) message.body();
      String playerName = currentMove.getPlayerName();
      currentMove = bodyJson.mapTo(Move.class);
      currentMove.setPlayerName(playerName);

      System.out.println("* player " + currentMove.getOpponentName()
        + " moved: "
        + currentMove.getOpponentsMove()
        + " and their resulting number is: "
        + currentMove.getOpponentsResult());

      System.out.println("please make your move(move:(-1/0/1)):");
      message.reply(Boolean.FALSE);
    }

  }

  private void handleGameStatus(Message<Object> message) {
    LOGGER.info("received game status request");
    message.reply(JsonObject.mapFrom(currentMove));
  }

  private void printCommands() {
    System.out.println("player:yourName -> sets name in game");
    System.out.println("game_mode:automatic -> sets game mode to automatic, if you want to start type begin afterwards");
    System.out.println("game_mode:manual -> sets game mode to manual, make your move according to number");
    System.out.println("move:-1 -> plays -1, you can also write 1 and 0 according to the number provided");
    System.out.println("reset -> resets name and the game mode");
    System.out.println("begin -> begins game in automatic mode");
    System.out.println("help -> available commands");
  }

  private HashSet<String> initCommandSet() {
    HashSet<String> commandSet = new HashSet<>();
    commandSet.add("game_mode");
    commandSet.add("player");
    commandSet.add("move");
    commandSet.add("begin");
    commandSet.add("reset");
    commandSet.add("help");
    return commandSet;
  }
}
