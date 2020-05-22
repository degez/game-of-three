package com.yucel.challenge.gameofthree.service;

import com.yucel.challenge.gameofthree.model.Move;

public interface MoveCalculator {
  Move calculateNextMove(Move move);
}
