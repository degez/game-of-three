package com.yucel.challenge.gameofthree.model;

public enum GameType {
  MANUAL("manual"),
  AUTOMATIC("automatic");

  String type;

  GameType(String type) {
    this.type = type;
  }

  public String getType(){
    return type;
  }
}
