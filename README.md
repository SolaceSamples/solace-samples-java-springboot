## Overview
This Repository contains examples demonstrating how to use the PubSub+ Messaging API for Java with Spring Boot, which is one of many ways to use Solace PubSub+ with Spring. Please see the [solace-samples-spring](https://github.com/SolaceSamples/solace-samples-spring) repository for how to use Solace PubSub+ with Spring Cloud Stream or how to use our Spring Boot Starters for JMS or JCSMP. 

## Exploring the Samples

### Samples Descriptions
This repository consists of 4 smaller applications :
- spring-boot-datamodel : This app creates and installs a maven artifact containing datamodels which are used in the other applications
- spring-boot-api-producer : This application exposes a REST API which connects to the Solace broker and publishes an event in the topic configured in its properties files
- spring-boot-consumer : This application defines a Spring Boot microservice which functions as a consumer for the events that are published by the `spring-boot-api-producer` application
- spring-boot-processor : This application functions as a Processor i.e. simultaneously consumes and produces events. The events which are published by the `spring-boot-api-producer` are consumed and in turn a processed message is published to a new topic.

## Prerequisites
### Access a PubSub+ Service
Before running the samples in this repository you will need to have access to a PubSub+ Messaging service. 
There are three ways you can get started:
- Follow [these instructions](https://docs.solace.com/Cloud/ggs_create_first_service.htm) to quickly spin up a cloud-based Solace messaging service for your applications.
- Follow [these instructions](https://docs.solace.com/Software-Broker/SW-Broker-Set-Up/Containers/Set-Up-Container-Image.htm) to start the Solace VMR in leading Clouds, Container Platforms or Hypervisors. The tutorials outline where to download and how to install the Solace VMR.
- If your company has Solace message routers deployed, contact your middleware team to obtain the host name or IP address of a Solace message router to test against, a username and password to access it, and a VPN in which you can produce and consume messages.

### Install the data model
Using maven, install the data model to your maven repository
``` bash
cd spring-boot-datamodel
mvnw clean install
```

## Running the Samples
To try individual samples, go into the project directory and run the sample using maven.

``` bash
cd spring-boot-consumer
mvnw clean spring-boot:run
```
See the individual code samples linked from the [springboot code samples](https://github.com/SolaceSamples/solace-samples-java-springboot/) for full details which can walk you through the samples, what they do, and how to correctly run them to explore Spring

## Setting up your preferred IDE
Using a modern Java IDE provides cool productivity features like auto-completion, on-the-fly compilation, assisted refactoring and debugging which can be useful when you're exploring the samples and even modifying the samples. Follow the steps below for your preferred IDE. This repository uses Maven projects. If you would like to import the projects into your favorite IDE you should be able to import them as Maven Projects. For examples, in eclipse choose "File -> Import -> Maven -> Existing Maven Projects -> Next -> Browse for your repo -> Select which projects -> Click Finish" </br>

## Contributing
Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on our code of conduct, and the process for submitting pull requests to us.

## Authors
See the list of [contributors](https://github.com/SolaceSamples/solace-samples-java-springboot/contributors) who participated in this project.

## License
This project is licensed under the Apache License, Version 2.0. - See the [LICENSE](LICENSE) file for details.

## Resources
For more information try these resources:
- Join the [Solace Community](https://solace.community)
- [Tutorials](https://tutorials.solace.dev/)
- The Solace Developer Portal website at: [Developer Portal](https://solace.dev)
