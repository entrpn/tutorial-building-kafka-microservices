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

```json
{"id":"1","customerId":1,"state":"CREATED","product":"JUMPERS","quantity":2,"price":12.99}
``` 

### Example_2 Create order validator consumer and producer

Create OrderDetailsService that listens to orders topic and validates order. Produces message to orders-validated topic with a pass/fail criteria.
The json deserializer is created in this step. Also start using enums in the messages instead of only primitives.

1. Run all the steps in Example_1

2. Create order-validations topic

```./bin/kafka-topics --create --zookeeper localhost:2181 --partitions 1 --replication-factor 1 --topic order-validations```

3. Run the OrderDetailsService:

```mvn exec:java -f pom.xml -Dexec.mainClass=com.entrpn.examples.kafka.streams.microservices.OrderDetailsService -Dexec.args="localhost:9092 http://localhost:8081"```

3. Subscribe to the order-validations topic and view validationResult

```./bin/kafka-console-consumer --bootstrap-server localhost:9092 --topic order-validations --from-beginning```

You should see a message through the console:

```json
{"orderId":"545454","checkType":"ORDER_DETAILS_CHECK","validationResult":"PASS"}
```

4. Send more requests to view more results:

```curl -d '{"id":"1","customerId":"1","state":"CREATED","product":"JUMPERS","quantity":"2","price":"12.99"}' -X POST http://10.70.29.54:26601/v1/orders --header "Content-Type: application/json"```


### Example_3a

Create an email service that joins the topics orders and payments and joins on the orderId. Prints the contents of both topics.
In the next excercise (Example_3b), we will join with the customers topic and simulate sending an email to a customer once the payment for an order (again, joined by orderId) is completed.

1. Create a payments topic. Topics that join must have the same number of partitions:

```./bin/kafka-topics --create --zookeeper localhost:2181 --partitions 1 --replication-factor 1 --topic payments```

2. Run the EmailService:

```mvn exec:java -Dexec.mainClass=com.entrpn.examples.kafka.streams.microservices.EmailService "-Dexec.args=localhost:9092 http://localhost:8081" -f pom.xml ```

3. Produce orders and Payments:

```mvn exec:java -f pom.xml -Dexec.mainClass=com.entrpn.examples.kafka.streams.microservices.util.ProduceOrders```

```mvn exec:java -f pom.xml -Dexec.mainClass=com.entrpn.examples.kafka.streams.microservices.util.ProducePayments```

Look at the logs for the output. When the orderId matches in both topics: 

```text
[2019-09-24 15:23:46,446] INFO [EmailService-a4bff565-2333-4bb0-a6b3-f9e75c34ec04-StreamThread-1] ************************ (com.entrpn.examples.kafka.streams.microservices.EmailService)
[2019-09-24 15:23:46,446] INFO [EmailService-a4bff565-2333-4bb0-a6b3-f9e75c34ec04-StreamThread-1] key (orderId): 50 (com.entrpn.examples.kafka.streams.microservices.EmailService)
[2019-09-24 15:23:46,446] INFO [EmailService-a4bff565-2333-4bb0-a6b3-f9e75c34ec04-StreamThread-1] payment.orderId: 50 (com.entrpn.examples.kafka.streams.microservices.EmailService)
[2019-09-24 15:23:46,446] INFO [EmailService-a4bff565-2333-4bb0-a6b3-f9e75c34ec04-StreamThread-1] order.orderId: 50 (com.entrpn.examples.kafka.streams.microservices.EmailService)
[2019-09-24 15:23:46,446] INFO [EmailService-a4bff565-2333-4bb0-a6b3-f9e75c34ec04-StreamThread-1] customerId: 15 (com.entrpn.examples.kafka.streams.microservices.EmailService)
[2019-09-24 15:23:46,447] INFO [EmailService-a4bff565-2333-4bb0-a6b3-f9e75c34ec04-StreamThread-1] orderState: CREATED (com.entrpn.examples.kafka.streams.microservices.EmailService)
[2019-09-24 15:23:46,447] INFO [EmailService-a4bff565-2333-4bb0-a6b3-f9e75c34ec04-StreamThread-1] product: UNDERPANTS (com.entrpn.examples.kafka.streams.microservices.EmailService)
[2019-09-24 15:23:46,447] INFO [EmailService-a4bff565-2333-4bb0-a6b3-f9e75c34ec04-StreamThread-1] quantity: 3 (com.entrpn.examples.kafka.streams.microservices.EmailService)
[2019-09-24 15:23:46,447] INFO [EmailService-a4bff565-2333-4bb0-a6b3-f9e75c34ec04-StreamThread-1] price: 5.0 (com.entrpn.examples.kafka.streams.microservices.EmailService)
[2019-09-24 15:23:46,447] INFO [EmailService-a4bff565-2333-4bb0-a6b3-f9e75c34ec04-StreamThread-1] getAmount: 1000.0 (com.entrpn.examples.kafka.streams.microservices.EmailService)
[2019-09-24 15:23:46,447] INFO [EmailService-a4bff565-2333-4bb0-a6b3-f9e75c34ec04-StreamThread-1] ccy: CZK (com.entrpn.examples.kafka.streams.microservices.EmailService)
```

### Example_3b

Create a GlobalKTable. Join the topic customers to orders and payments. Simulate sending an email whenever the payment and order are received.
Create a message based on the customer's level and send it to that topic.

1. After checking out example_3b, build the app:

```mvn install -Dmaven.test.skip=true```

2.  Create a customers topic:

```./bin/kafka-topics --create --zookeeper localhost:2181 --partitions 1 --replication-factor 1 --topic payments```

3. Create the 3 topics that represent customer levels (gold, silver, platinum):

```./bin/kafka-topics --create --zookeeper localhost:2181 --partitions 1 --replication-factor 1 --topic gold```

```./bin/kafka-topics --create --zookeeper localhost:2181 --partitions 1 --replication-factor 1 --topic silver```

```./bin/kafka-topics --create --zookeeper localhost:2181 --partitions 1 --replication-factor 1 --topic platinum```

4. Run the EmailService:

```mvn exec:java -Dexec.mainClass=com.entrpn.examples.kafka.streams.microservices.EmailService "-Dexec.args=localhost:9092 http://localhost:8081" -f pom.xml ```

5. Produce order, payment and customer records:

```mvn exec:java -f pom.xml -Dexec.mainClass=com.entrpn.examples.kafka.streams.microservices.util.ProduceOrders```

```mvn exec:java -f pom.xml -Dexec.mainClass=com.entrpn.examples.kafka.streams.microservices.util.ProducePayments```

```mvn exec:java -f pom.xml -Dexec.mainClass=com.entrpn.examples.kafka.streams.microservices.util.ProduceCustomers```

6. Listen to the orders, customers, payments and gold topics to view messages

```./bin/kafka-console-consumer --bootstrap-server localhost:9092 --topic orders --from-beginning```

```./bin/kafka-console-consumer --bootstrap-server localhost:9092 --topic payments --from-beginning```

```./bin/kafka-console-consumer --bootstrap-server localhost:9092 --topic customers --from-beginning```

```./bin/kafka-console-consumer --bootstrap-server localhost:9092 --topic gold --from-beginning```

Notice the EmailService app logging. When messages from orders, payments and customers match on the orderId and cstomerId the sendEmail method is called to simulate sending an email:

```text
19-09-27 09:40:42,736] INFO [EmailService-43212396-9a2e-446b-809e-e0c77b92a015-StreamThread-1] ************************ (com.entrpn.examples.kafka.streams.microservices.EmailService)
[2019-09-27 09:40:42,737] INFO [EmailService-43212396-9a2e-446b-809e-e0c77b92a015-StreamThread-1] key (orderId): 50 (com.entrpn.examples.kafka.streams.microservices.EmailService)
[2019-09-27 09:40:42,737] INFO [EmailService-43212396-9a2e-446b-809e-e0c77b92a015-StreamThread-1] payment.orderId: 50 (com.entrpn.examples.kafka.streams.microservices.EmailService)
[2019-09-27 09:40:42,737] INFO [EmailService-43212396-9a2e-446b-809e-e0c77b92a015-StreamThread-1] order.orderId: 50 (com.entrpn.examples.kafka.streams.microservices.EmailService)
[2019-09-27 09:40:42,737] INFO [EmailService-43212396-9a2e-446b-809e-e0c77b92a015-StreamThread-1] customerId: 15 (com.entrpn.examples.kafka.streams.microservices.EmailService)
[2019-09-27 09:40:42,737] INFO [EmailService-43212396-9a2e-446b-809e-e0c77b92a015-StreamThread-1] orderState: CREATED (com.entrpn.examples.kafka.streams.microservices.EmailService)
[2019-09-27 09:40:42,737] INFO [EmailService-43212396-9a2e-446b-809e-e0c77b92a015-StreamThread-1] product: UNDERPANTS (com.entrpn.examples.kafka.streams.microservices.EmailService)
[2019-09-27 09:40:42,737] INFO [EmailService-43212396-9a2e-446b-809e-e0c77b92a015-StreamThread-1] quantity: 3 (com.entrpn.examples.kafka.streams.microservices.EmailService)
[2019-09-27 09:40:42,737] INFO [EmailService-43212396-9a2e-446b-809e-e0c77b92a015-StreamThread-1] price: 5.0 (com.entrpn.examples.kafka.streams.microservices.EmailService)
[2019-09-27 09:40:42,737] INFO [EmailService-43212396-9a2e-446b-809e-e0c77b92a015-StreamThread-1] getAmount: 1000.0 (com.entrpn.examples.kafka.streams.microservices.EmailService)
[2019-09-27 09:40:42,737] INFO [EmailService-43212396-9a2e-446b-809e-e0c77b92a015-StreamThread-1] ccy: CZK (com.entrpn.examples.kafka.streams.microservices.EmailService)
[2019-09-27 09:40:42,737] WARN [EmailService-43212396-9a2e-446b-809e-e0c77b92a015-StreamThread-1] Sending email: 
Customer:com.entrpn.examples.kafka.streams.microservices.dtos.Customer@1deda9ad
Order:com.entrpn.examples.kafka.streams.microservices.dtos.Order@2932e8fb
Paymentcom.entrpn.examples.kafka.streams.microservices.dtos.Payment@d88b470 (com.entrpn.examples.kafka.streams.microservices.EmailService)
```

Also the gold topic receives a message:

```json
{"id":"131","customerId":15,"customerLevel":"gold"}
```

### Example_4

Here you create a FraudService that filters orders in "CREATED" state and aggregates how much money is being spent for that customer over a period of 1 hour.
If that amount spent goes over a threshold then the consequent order fails based on the possibility of fraud. This is done via branching. A message is posted 
into order-validations topic with a PASS or FAIL for the FRAUD_CHECK validation type (remember you built validation type ORDER_DETAILS_CHECK in example 2).

1. After checking out example_4, build the app:

```mvn install -Dmaven.test.skip=true```

2. Run FraudService:

```mvn exec:java -Dexec.mainClass=com.entrpn.examples.kafka.streams.microservices.FraudService "-Dexec.args=localhost:9092 http://localhost:8081" -f pom.xml ```

3. Send orders:

```mvn exec:java -f pom.xml -Dexec.mainClass=com.entrpn.examples.kafka.streams.microservices.util.ProduceOrders```

In my case I had a lot of orders in the backlog so I received a failure right away, but your results might be different if you don't have any messages in the orders topic and the KTable.

```json
{"orderId":"50","checkType":"FRAUD_CHECK","validationResult":"FAIL"}
```

You can delete your KTable and orders topic, recreate it and play aroudn with ProduceOrders to see transactions pass then fail.

