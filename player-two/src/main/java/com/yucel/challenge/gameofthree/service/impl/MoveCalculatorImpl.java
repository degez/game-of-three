package com.yucel.challenge.gameofthree.service.impl;

import com.yucel.challenge.gameofthree.model.Move;
import com.yucel.challenge.gameofthree.service.MoveCalculator;

import java.util.HashMap;
import java.util.Map;
import java.util.function.IntFunction;

public class MoveCalculatorImpl implements MoveCalculator {
  private final Map<Integer, IntFunction<Integer>> operationMap = new HashMap();
  private final Map<Integer, Integer> moveMap = new HashMap<>();

  public MoveCalculatorImpl() {
    operationMap.put(0, input -> input / 3);
    operationMap.put(1, input -> (input - 1) / 3);
    operationMap.put(2, input -> (input + 1) / 3);

    moveMap.put(0, 0);
    moveMap.put(1, -1);
    moveMap.put(2, 1);

  }

  public Move calculateNextMove(Move move){
    Integer opponentsResult = move.getOpponentsResult();

    Integer remainder = opponentsResult % 3;

    move.setResult(operationMap.get(remainder).apply(opponentsResult));
    move.setMove(moveMap.get(remainder));
    return move;
  };
}
