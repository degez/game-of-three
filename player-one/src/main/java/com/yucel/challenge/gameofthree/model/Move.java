package com.yucel.challenge.gameofthree.model;

import java.util.concurrent.ThreadLocalRandom;

public class Move {
  private String playerName;
  private Integer move;
  private Integer result;
  private Integer opponentsMove;
  private Integer opponentsResult;
  private String opponentName;

  public Move() {
  }
  public Move(String playerName) {
    this.playerName = playerName;
  }

  public Move(String playerName, Integer move, Integer result, Integer opponentsMove, Integer opponentsResult) {
    this.playerName = playerName;
    this.move = move;
    this.result = result;
    this.opponentsMove = opponentsMove;
    this.opponentsResult = opponentsResult;
  }

  public String getPlayerName() {
    return playerName;
  }

  public void setPlayerName(String playerName) {
    this.playerName = playerName;
  }

  public Integer getMove() {
    return move;
  }

  public void setMove(Integer move) {
    this.move = move;
  }

  public Integer getOpponentsMove() {
    return opponentsMove;
  }

  public void setOpponentsMove(Integer opponentsMove) {
    this.opponentsMove = opponentsMove;
  }

  public Integer getResult() {
    if(result == null){
      result = ThreadLocalRandom.current().nextInt(2, 100);
    }
    return result;
  }

  public void setResult(Integer result) {
    this.result = result;
  }

  public Integer getOpponentsResult() {
    if(opponentsResult == null){
      opponentsResult = getResult();
    }
    return opponentsResult;
  }

  public void setOpponentsResult(Integer opponentsResult) {
    this.opponentsResult = opponentsResult;
  }

  public String getOpponentName() {
    return opponentName;
  }

  public void setOpponentName(String opponentName) {
    this.opponentName = opponentName;
  }

  @Override
  public String toString() {
    return "Move{" +
      "playerName='" + playerName + '\'' +
      ", move=" + move +
      ", result=" + result +
      ", opponentsMove=" + opponentsMove +
      ", opponentsResult=" + opponentsResult +
      ", opponentName='" + opponentName + '\'' +
      '}';
  }
}
