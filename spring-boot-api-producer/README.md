## Contents

This sample exposes a simple Springboot based REST API which then publishes an event to a Solace topic using the new
Java API.

## Prerequisites

Install the data model

``` bash
cd spring-boot-datamodel
mvn clean install
```

See the individual tutorials linked from
the [tutorials home page](https://github.com/SolaceSamples/solace-samples-springboot/) for full details which can walk
you through the samples, what they do, and how to correctly run them to explore Spring

## Exploring the Samples

This sample exposes a simple REST API `/solace/samples/spring/boot/producer/sensor/reading` </br>
Present in this repo is a postman collection which can be used to test out this API.

The configuration by default produces events to the topic : `solace/samples/java/direct/pub/` </br>.
To demonstrate the capabilities of the dynamic topics, the topic string is finalized by using inputs from the incoming
request body.<br>
To run the application, update the configuration parameters in the file application.yml. </br>
The required connection parameters can be found in the broker Connect tab's : **Solace Messaging**
section : [Connection Parameters Image](readmeImages/connectionParameters.png)</br>
To inspect the messages being produced, subscribe to the configured topic in the broker console's **Try-Me**
tab : [Subscriber Connection Image](readmeImages/subscriberImage.png)<br>

### Setting up your preferred IDE

Using a modern Java IDE provides cool productivity features like auto-completion, on-the-fly compilation, assisted
refactoring and debugging which can be useful when you're exploring the samples and even modifying the samples. Follow
the steps below for your preferred IDE.
This repository uses Maven projects. If you would like to import the projects into your favorite IDE you should be able
to import them as Maven Projects. For examples, in eclipse choose "File -> Import -> Maven -> Existing Maven Projects ->
Next -> Browse for your repo -> Select which projects -> Click Finish"
