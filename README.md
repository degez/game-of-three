# Game of Three

The Goal is to implement a game with two independent units – the players –
communicating with each other using an API

## Getting Started

You need Java 8 on your environment. Project can be installed with maven and developed in Intellij IDEA.

Vert.x toolkit is used to build microservices, it is a lightweight and flexible toolkit, an event-driven and non-blocking library set.
For testing Vert.x JUnit 5 is used.
For dependency injection Guice framework is used.


### Installing and Packaging

You can navigate to the projects' directory(player-one and player-two) and run the commands below


== Building

To launch the tests:
```
./mvnw clean test
```

To package the application:
```
./mvnw clean package
```

### Running services
To run the application:
```
./mvnw clean compile exec:java
```


This commands run fat jar file as alternative, to run them:

```
java -jar target/player-one-1.0.0-SNAPSHOT-fat.jar
java -jar target/player-two-1.0.0-SNAPSHOT-fat.jar

```


## About the Project

Vert.x provides an event bus, I initialize vert.x instance clustered on both of the applications' Main class.
While doing that I provide a cluster manager, Hazelcast in my case, and Vert.x finds other instances by TCP calls on network and joins them.
On local machine this configuration is enough for different instances to join each other, but on different servers extra configuration may be required, in that case we need to provide a cluster.xml file.

All Verticles are capable of running on their own, so I didn't rely on shared constants too much.

I tried to avoid anemic domain model. Some basic operations are in the model etc.

Command line processor blocks a thread, so it is occupying a worker thread.
Vert.x has 4 threads per core for event-loop thread groups, it is important not to block them. So it provides us worker threads for this kind of executions.

### Points could be improved

Because of my choice of command line inputs there are lots of conditions, sorry for that :) Could refactor on Command, but would take time.

The player-one and player-two projects are almost identical as code. There are a few variables named differently.
There could be another iteration to pass them all to configuration file, and manage all instances of players with the same codebase.
Still, this would take some more time because Vert.x service discovery needs to be involved, we can specify instance counts on deployment with vert.x easily,
but with different deployments we cannot deploy a verticle with the same consumer address, so when we make them dynamic, then we also should implement a way to make an instance find another without knowing it's address.

Vert.x is a fast communication tool between services, still more complex if we compare SpringBoot, high usage of Async calls and handler. And because of this testing is also tricky.
I only provided integration tests for very limited operations, since I invested a couple of days already I choose to skip covering scenarios. Especially on integration tests most of the cases services needs to communicate each other and that is an investigation task for me.
Logging and validation could also be improved.

All messages could be retrieved from property source.

A session could be built for the move history.

Javadoc should be added.


## Usage of the Project

After we run "player-one" and "player-two", we can do these thins:
* player one and player two can be both automatic
* player one and player two can be both manual
* player one can be manual and player two can be automatic
* player two can be manual and player one can be automatic

Commands:
```
player:yourName -> sets name in game
game_mode:automatic -> sets game mode to automatic, if you want to start type begin afterwards
game_mode:manual -> sets game mode to manual, make your move according to number
move:-1 -> plays -1, you can also write 1 and 0 according to the number provided
reset -> resets name and the game mode
begin -> begins game in automatic mode
help -> available commands
```

Application checks if other instance is up before sending move message.
There are two cases:
1. If other application is down: "no opponent application available, please start the other instance - cause"
1. If other player haven't set his/her name and didn't selected a game_mode:

Steps:
1. You must provide your name
1. You must select one of the game modes (game_mode:automatic or game_mode:manual)
    1. If in automatic mode, you can start the game with opponent by the input "begin". It will send a random number automaticly, if opponent is human, you need to wait the move event to be send. If it is also AI the game will continue until one of the players win. Communication is pretty fast, you can manipulate Move objects random operation to a high number.
    1. If in manual mode, you will see a number(starting number: 99), and make your move accordingly. Ex. for 11 -> move:1
        1. then you will receive a reply, and go on until someone wins.
1. When the game ends, you need to reset and start from item 1
1. If player not available you can make the same move again and try.

Validations:
* move:X -> X needs to be one of -1, 0, 1
* move:X -> after providedNumber+X sum, the sum needs to be divided by 3 without remainder.
* move:X -> X needs to be integer
* game_mode:XXX -> XXX needs to be one of these; manual, automatic
* commands should be one of the provided list.

You can find more traditional projects with Spring Boot on my GitHub:
https://github.com/degez?tab=repositories
repayment-kata and bayes-java-code-challenge-1 are the recent ones.

Thank you very much, and sorry for not having time for more refactoring!


