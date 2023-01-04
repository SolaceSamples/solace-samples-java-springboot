## Springboot Overview

The Spring Framework is an application framework and inversion of control container for the Java platform.  Dating back to the early 2000s the Spring ecosystem has grown tremendously and has become the most popular framework for Enterprise Java Developers.
</br>Spring Boot is an "opinionated" application bootstrapping framework that makes it easy to create new RESTful services (among other types of applications).
</br>Spring Boot makes it easy to create stand-alone, production-grade Spring based Applications that you can "just run".

- https://spring.io/
- https://spring.io/projects/spring-boot

## Solace PubSub+ Spring

At Solace, we support integrating our PubSub+ Event Broker with Spring in several ways to promote the ease of building event driven applications. 
This repository contains code samples for doing so, but it's best to clone the repo :
* Spring Boot REST Producer 
* Spring Boot Consumer
* Spring Boot Processor

## Access a PubSub+ Service

The "Getting Started" tutorials will get you up to speed and sending messages with Solace technology as quickly as possible. There are three ways you can get started:

- Follow [these instructions](https://cloud.solace.com/learn/group_getting_started/ggs_signup.html) to quickly spin up a cloud-based Solace messaging service for your applications.
- Follow [these instructions](https://docs.solace.com/Solace-SW-Broker-Set-Up/Setting-Up-SW-Brokers.htm) to start the Solace VMR in leading Clouds, Container Platforms or Hypervisors. The tutorials outline where to download and how to install the Solace VMR.
- If your company has Solace message routers deployed, contact your middleware team to obtain the host name or IP address of a Solace message router to test against, a username and password to access it, and a VPN in which you can produce and consume messages.
## Contents

This repository contains code and matching tutorial walk through for different basic scenarios using the Solace PubSub+ Messaging API for Java (not the JCSMP)
It is best to view the associated [tutorials home page](https://tutorials.solace.dev/).

## Prerequisites

Install the data model
``` bash
cd spring-boot-datamodel
mvn clean install
```

## Running the Samples

To try individual samples, go into the project directory and run the sample using maven.

``` bash
cd spring-boot-consumer
mvn clean spring-boot:run
```
See the individual code samples linked from the [springboot code samples](https://github.com/SolaceSamples/solace-samples-springboot/) for full details which can walk you through the samples, what they do, and how to correctly run them to explore Spring

## Exploring the Samples

### Setting up your preferred IDE

Using a modern Java IDE provides cool productivity features like auto-completion, on-the-fly compilation, assisted refactoring and debugging which can be useful when you're exploring the samples and even modifying the samples. Follow the steps below for your preferred IDE. </br>
This repository uses Maven projects. If you would like to import the projects into your favorite IDE you should be able to import them as Maven Projects. For examples, in eclipse choose "File -> Import -> Maven -> Existing Maven Projects -> Next -> Browse for your repo -> Select which projects -> Click Finish" </br>

### Highlevel sample description

This repository consists of 4 smaller applications :

- spring-boot-datamodel : This app creates and installs a maven artifact containing datamodels which are used in the other applications </br>
- spring-boot-api-producer : This application exposes a REST API which connects to the Solace broker and publishes an event in the topic configured in its properties files </br>
- spring-boot-consumer : This application defines a Springboot based microservices which functions as a consumer for the events that are published by the `spring-boot-api-producer` application </br>
- spring-boot-processor : This application functions as a Processor i.e. simultaneously consumes and produces events. The events which are published by the `spring-boot-api-producer`
                            are consumed and in turn a reply message is published to a new topic.

## Contributing

Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on our code of conduct, and the process for submitting pull requests to us.

## Authors

See the list of [contributors](https://github.com/SolaceSamples/solace-samples-springboot/contributors) who participated in this project.

## License

This project is licensed under the Apache License, Version 2.0. - See the [LICENSE](LICENSE) file for details.

## Resources

For more information try these resources:

- [Tutorials](https://tutorials.solace.dev/)
- The Solace Developer Portal website at: [Developer Portal](http://solace.com/developers)
- Check out the [Solace blog](https://solace.com/blog/category/developers/) for other interesting discussions around Solace technology
- Follow our Developer Advocates on Twitter for development tips & to keep up with the latest Solace news! [@SolaceDevs](https://twitter.com/solacedevs)
