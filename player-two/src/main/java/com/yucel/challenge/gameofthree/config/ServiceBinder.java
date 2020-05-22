package com.yucel.challenge.gameofthree.config;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.yucel.challenge.gameofthree.service.MoveCalculator;
import com.yucel.challenge.gameofthree.service.impl.MoveCalculatorImpl;

public class ServiceBinder extends AbstractModule {

  @Override
  protected void configure() {
    bind(MoveCalculator.class).to(MoveCalculatorImpl.class).in(Singleton.class);
  }
}
