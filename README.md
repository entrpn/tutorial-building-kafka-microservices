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

In this step a producer is created. When a create orderBean request is received, it sends the Order body as a message over the topic orders.
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

### Example_2 Create orderBean validator consumer and producer

Create OrderDetailsService that listens to orders topic and validates orderBean. Produces message to orders-validated topic with a pass/fail criteria.
The json deserializer is created in this step. Also start using enums in the messages instead of only primitives.

1. Run all the steps in Example_1

2. Create orderBean-validations topic

```./bin/kafka-topics --create --zookeeper localhost:2181 --partitions 1 --replication-factor 1 --topic orderBean-validations```

3. Run the OrderDetailsService:

```mvn exec:java -f pom.xml -Dexec.mainClass=com.entrpn.examples.kafka.streams.microservices.OrderDetailsService -Dexec.args="localhost:9092 http://localhost:8081"```

3. Subscribe to the orderBean-validations topic and view validationResult

```./bin/kafka-console-consumer --bootstrap-server localhost:9092 --topic orderBean-validations --from-beginning```

You should see a message through the console:

```json
{"orderId":"545454","checkType":"ORDER_DETAILS_CHECK","validationResult":"PASS"}
```

4. Send more requests to view more results:

```curl -d '{"id":"1","customerId":"1","state":"CREATED","product":"JUMPERS","quantity":"2","price":"12.99"}' -X POST http://10.70.29.54:26601/v1/orders --header "Content-Type: application/json"```


### Example_3a

Create an email service that joins the topics orders and payments and joins on the orderId. Prints the contents of both topics.
In the next excercise (Example_3b), we will join with the customers topic and simulate sending an email to a customer once the payment for an orderBean (again, joined by orderId) is completed.

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
[2019-09-24 15:23:46,446] INFO [EmailService-a4bff565-2333-4bb0-a6b3-f9e75c34ec04-StreamThread-1] orderBean.orderId: 50 (com.entrpn.examples.kafka.streams.microservices.EmailService)
[2019-09-24 15:23:46,446] INFO [EmailService-a4bff565-2333-4bb0-a6b3-f9e75c34ec04-StreamThread-1] customerId: 15 (com.entrpn.examples.kafka.streams.microservices.EmailService)
[2019-09-24 15:23:46,447] INFO [EmailService-a4bff565-2333-4bb0-a6b3-f9e75c34ec04-StreamThread-1] orderState: CREATED (com.entrpn.examples.kafka.streams.microservices.EmailService)
[2019-09-24 15:23:46,447] INFO [EmailService-a4bff565-2333-4bb0-a6b3-f9e75c34ec04-StreamThread-1] product: UNDERPANTS (com.entrpn.examples.kafka.streams.microservices.EmailService)
[2019-09-24 15:23:46,447] INFO [EmailService-a4bff565-2333-4bb0-a6b3-f9e75c34ec04-StreamThread-1] quantity: 3 (com.entrpn.examples.kafka.streams.microservices.EmailService)
[2019-09-24 15:23:46,447] INFO [EmailService-a4bff565-2333-4bb0-a6b3-f9e75c34ec04-StreamThread-1] price: 5.0 (com.entrpn.examples.kafka.streams.microservices.EmailService)
[2019-09-24 15:23:46,447] INFO [EmailService-a4bff565-2333-4bb0-a6b3-f9e75c34ec04-StreamThread-1] getAmount: 1000.0 (com.entrpn.examples.kafka.streams.microservices.EmailService)
[2019-09-24 15:23:46,447] INFO [EmailService-a4bff565-2333-4bb0-a6b3-f9e75c34ec04-StreamThread-1] ccy: CZK (com.entrpn.examples.kafka.streams.microservices.EmailService)
```

### Example_3b

Create a GlobalKTable. Join the topic customers to orders and payments. Simulate sending an email whenever the payment and orderBean are received.
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

5. Produce orderBean, payment and customer records:

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
[2019-09-27 09:40:42,737] INFO [EmailService-43212396-9a2e-446b-809e-e0c77b92a015-StreamThread-1] orderBean.orderId: 50 (com.entrpn.examples.kafka.streams.microservices.EmailService)
[2019-09-27 09:40:42,737] INFO [EmailService-43212396-9a2e-446b-809e-e0c77b92a015-StreamThread-1] customerId: 15 (com.entrpn.examples.kafka.streams.microservices.EmailService)
[2019-09-27 09:40:42,737] INFO [EmailService-43212396-9a2e-446b-809e-e0c77b92a015-StreamThread-1] orderState: CREATED (com.entrpn.examples.kafka.streams.microservices.EmailService)
[2019-09-27 09:40:42,737] INFO [EmailService-43212396-9a2e-446b-809e-e0c77b92a015-StreamThread-1] product: UNDERPANTS (com.entrpn.examples.kafka.streams.microservices.EmailService)
[2019-09-27 09:40:42,737] INFO [EmailService-43212396-9a2e-446b-809e-e0c77b92a015-StreamThread-1] quantity: 3 (com.entrpn.examples.kafka.streams.microservices.EmailService)
[2019-09-27 09:40:42,737] INFO [EmailService-43212396-9a2e-446b-809e-e0c77b92a015-StreamThread-1] price: 5.0 (com.entrpn.examples.kafka.streams.microservices.EmailService)
[2019-09-27 09:40:42,737] INFO [EmailService-43212396-9a2e-446b-809e-e0c77b92a015-StreamThread-1] getAmount: 1000.0 (com.entrpn.examples.kafka.streams.microservices.EmailService)
[2019-09-27 09:40:42,737] INFO [EmailService-43212396-9a2e-446b-809e-e0c77b92a015-StreamThread-1] ccy: CZK (com.entrpn.examples.kafka.streams.microservices.EmailService)
[2019-09-27 09:40:42,737] WARN [EmailService-43212396-9a2e-446b-809e-e0c77b92a015-StreamThread-1] Sending email: 
Customer:com.entrpn.examples.kafka.streams.microservices.dtos.Customer@1deda9ad
Order:com.entrpn.examples.kafka.streams.microservices.dtos.OrderBean@2932e8fb
Paymentcom.entrpn.examples.kafka.streams.microservices.dtos.Payment@d88b470 (com.entrpn.examples.kafka.streams.microservices.EmailService)
```

Also the gold topic receives a message:

```json
{"id":"131","customerId":15,"customerLevel":"gold"}
```

### Example_4

Here you create a FraudService that filters orders in "CREATED" state and aggregates how much money is being spent for that customer over a period of 1 hour.
If that amount spent goes over a threshold then the consequent orderBean fails based on the possibility of fraud. This is done via branching. A message is posted 
into orderBean-validations topic with a PASS or FAIL for the FRAUD_CHECK validation type (remember you built validation type ORDER_DETAILS_CHECK in example 2).

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

You can delete your KTable and orders topic, recreate it and play around with ProduceOrders to see transactions pass then fail.

### Example_5

Here you create a ValidationsAggregatorService that uses stateful operations. They are stateful because they maintain data during processing. 
In this exercise a 2 minute window is used for processing. If all 3 successful operations are received for an orderBean (INVENTORY_CHECK, FRAUD_CHECK, ORDER_DETAILS_CHECK)
then the orderBean moves to the validated state, else in the failed state.

1. After checking out example_5, build the app:

```mvn install -Dmaven.test.skip=true```

2. Run OrderDetailsService, OrdersService and ValidationAggregatorService:

```exec:java -Dexec.mainClass=com.entrpn.examples.kafka.streams.microservices.OrderDetailsService "-Dexec.args=localhost:9092 http://localhost:8081" -f pom.xml```

```exec:java -Dexec.mainClass=com.entrpn.examples.kafka.streams.microservices.OrdersService "-Dexec.args=localhost:9092 http://localhost:8081 localhost 26601" -f pom.xml```

```exec:java -Dexec.mainClass=com.entrpn.examples.kafka.streams.microservices.ValidationsAggregatorService "-Dexec.args=localhost:9092 http://localhost:8081" -f pom.xml```

3. Listen to the orders and orderBean-validations topics

```./bin/kafka-console-consumer --bootstrap-server localhost:9092 --topic orders --from-beginning```

```./bin/kafka-console-consumer --bootstrap-server localhost:9092 --topic orderBean-validations --from-beginning```

4. Create an orderBean using CURL.

```curl -d '{"id":"4567","customerId":"1","state":"CREATED","product":"JUMPERS","quantity":"2","price":"12.99"}' -X POST http://10.70.29.54:26601/v1/orders --header "Content-Type: application/json"```

The orderBean can be viewed on the orders topic:

```json
{"id":"4567","customerId":1,"state":"CREATED","product":"JUMPERS","quantity":2,"price":12.99}
```

And the orderBean-validations:

```json
{"orderId":"545454","checkType":"ORDER_DETAILS_CHECK","validationResult":"PASS"}
```

5. The ValidationsAggregatorService only checks that a validationResult of PASS happens 3 times for a specific orderId. In reality we would check that each pass came from INVENTORY_CHECK, FRAUD_CHECK, ORDER_DETAILS_CHECK respectively.
Since that isn't the case, we can create two more orders with CURL to see the orderBean go from CREATED TO VALIDATED.

Run CURL two more times. Remember you have to create 3 orders with the same id within a 2 minute window for this to work.

The result should be in orders topic:

```json
{"id":"545454","customerId":1,"state":"VALIDATED","product":"JUMPERS","quantity":2,"price":12.99}
```

### Example_6

In this exercise, you will create a state store for the Inventory Service. This state store is initialized with data and updated as new orders are created.

1. After checking out example_6, build the app:

```mvn install -Dmaven.test.skip=true```

2. Create the topic warehouse-inventory

```./bin/kafka-topics --create --zookeeper localhost:2181 --partitions 1 --replication-factor 1 --topic warehouse-inventory```

3. Listen to the orderBean-validations topic

```./bin/kafka-console-consumer --bootstrap-server localhost:9092 --topic orderBean-validations --from-beginning```

4. Run the InventoryService

```mvn exec:java -Dexec.mainClass=com.entrpn.examples.kafka.streams.microservices.InventoryService -f pom.xml```

5. Add inventory by running AddInventory

``` mvn exec:java -Dexec.mainClass=com.entrpn.examples.kafka.streams.microservices.util.AddInventory -f pom.xml```

6. Run the OrderService:

```mvn exec:java -Dexec.mainClass=com.entrpn.examples.kafka.streams.microservices.OrdersService "-Dexec.args=localhost:9092 http://localhost:8081 localhost 26601" -f pom.xml```

7. Make an orderBean request:

```curl -d '{"id":"545454","customerId":"1","state":"CREATED","product":"JUMPERS","quantity":"2","price":"12.99"}' -X POST http://10.70.29.54:26601/v1/orders --header "Content-Type: application/json"```

The orderBean is validated in orderBean-validations topic because there is enough inventory.

```json
{"orderId":"545454","checkType":"INVENTORY_CHECK","validationResult":"PASS"}
```

Bonus: Try to create enough orders where validationResult fails due to not enough inventory.

### Example_7

In this exercise you start using avro for serialization/deserialization. Avro compacts the data https://www.confluent.io/blog/avro-kafka-data/

This exercise has a lot of changes, however, the main takeaways are: inside of the main folder, there is an avro folder that has the schemas for our records.
These schemas along with the avro-message-plugin, autogenerates the classes needed for serialization/deserialization.

All other changes in this exercise is refactoring from json to avro.

1. After checking out example_7, build the app:
   
```mvn install -Dmaven.test.skip=true```

2. 