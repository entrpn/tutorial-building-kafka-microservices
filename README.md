This repo is a set of walkable examples to learn how to build microservices with kafka streams.

Based on https://github.com/confluentinc/examples/tree/5.3.1-post/microservices-orders and broken down for beginners.

###Glossary

This project runs with Java 8

Start with each branch as follows:

-   example_1
-   example_2

### Example 0 - Build and run the app:

1. Check out, build, run:

```git checkout example_0```

```mvn install -Dmaven.test.skip=true```

```mvn exec:java -f pom.xml -Dexec.mainClass=com.entrpn.examples.kafka.streams.microservices.OrdersService -Dexec.args="localhost:9092 http://localhost:8081 localhost 26601"```

2. Test:

```curl -X POST http://10.70.29.54:26601/v1/orders```

Output should be: Hello World...

3. Move on to the next step:

```git checkout example_1```