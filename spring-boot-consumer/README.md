## Contents

This sample builds a simple Springboot based application consumes an event that is published to a Solace topic using the
new
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

This sample consumes events that have been published on the subscription `solace/samples/java/direct/pub/*` </br>
The corresponding producer application can be found on in this same project under the
repository [Spring-boot-api-producer](https://github.com/SolaceSamples/solace-samples-springboot/tree/main/spring-boot-api-producer)

To run the application, update the configuration parameters in the file application.yml. </br>
The required connection parameters can be found in the broker Connect tab's : **Solace Messaging**
section : [Connection Parameters Image](readmeImages/connectionParameters.png)</br>

### Setting up your preferred IDE

Using a modern Java IDE provides cool productivity features like auto-completion, on-the-fly compilation, assisted
refactoring and debugging which can be useful when you're exploring the samples and even modifying the samples. Follow
the steps below for your preferred IDE.
This repository uses Maven projects. If you would like to import the projects into your favorite IDE you should be able
to import them as Maven Projects. For examples, in eclipse choose "File -> Import -> Maven -> Existing Maven Projects ->
Next -> Browse for your repo -> Select which projects -> Click Finish"
