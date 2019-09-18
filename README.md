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

2. Test (Don't forget to change IP address to yours):

```curl -X POST http://10.70.29.54:26601/v1/orders```

Output should be: Hello World...

3. Move on to the next step:

```git checkout example_1```

### Example_1 - Create an orders producer

In this step a producer is created. When a create order request is received, it sends the Order body as a message over the topic orders.
To support sending the message a json serializer is created as well.

1. Download and unzip confluent https://www.confluent.io/download/. Version 5.3 is used in these tutorials.
2. Go to the confluent folder and run zookeeper, kafka. Might need to run as superuser

```./bin/zookeeper-server-start ./etc/kafka/zookeeper.properties```

```sudo ./bin/kafka-server-start ./etc/kafka/server.properties```

__Optional:__ start the schema registry (we will use this in a later example)

```./bin/schema-registry-start ./etc/schema-registry/schema-registry.properties```

3. Create an orders topic

```./bin/kafka-topics --create --zookeeper localhost:2181 --partitions 1 --replication-factor 1 --topic orders```

4. Check the topic exists. You should see an orders topic listed.

```./bin/kafka-topics --list --zookeeper localhost:2181```

5. Build and Run the app:

```` mvn install -Dmaven.test.skip=true -Dcheckstyle.skip````

```mvn exec:java -f pom.xml -Dexec.mainClass=com.entrpn.examples.kafka.streams.microservices.OrdersService -Dexec.args="localhost:9092 http://localhost:8081 localhost 26601"```

6. Launch a subscriber through console:

```./bin/kafka-console-consumer --bootstrap-server localhost:9092 --topic orders --from-beginning```

7. Send a request (Don't forget to change IP address to yours):

```curl -d '{"id":"1","customerId":"1","state":"CREATED","product":"JUMPERS","quantity":"2","price":"12.99"}' -X POST http://10.70.29.54:26601/v1/orders --header "Content-Type: application/json"```

You should receive a message through your console consumer as follows:

{"id":"1","customerId":1,"state":"CREATED","product":"JUMPERS","quantity":2,"price":12.99} 