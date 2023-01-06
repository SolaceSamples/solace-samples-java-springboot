## Contents

This sample exposes a simple Spring Boot based REST API which then publishes an event to a Solace topic using the Solace PubSub+ Messaging API for Java (not JCSMP).

## Prerequisites
Install the data model
``` bash
cd spring-boot-datamodel
mvnw clean install
```

## Configure PubSub+ Access
Add your messaging service client information to `applicaton.yml`. Find your connection info as seen in the [Connection Information section](#connection-information)

## Run the Sample
``` bash
cd spring-boot-api-producer
mvnw clean spring-boot:run
```

## Exploring the Samples
This sample exposes a simple REST API at `/solace/samples/spring/boot/producer/sensor/reading`
Present in this repo is a postman collection which can be used to test out this API.

The configuration by default produces events to the topic : `solace/samples/java/direct/pub/`. 

To demonstrate the capabilities of the dynamic topics, the topic string is finalized by using inputs from the incoming request body.

## Connection Information
This tutorial requires access Solace PubSub+ messaging and requires that you know several connectivity properties about your Solace messaging. Specifically you need to know the following:

| Resources       | Value  | Description                                                                                                                               |
|-----------------|--------|-------------------------------------------------------------------------------------------------------------------------------------------|
| Host            | String | This is the address clients use when connecting to the PubSub+ messaging to send and receive messages. (Format: DNS_NAME:Port or IP:Port) |
| Message VPN     | String | The PubSub+ message router Message VPN that this client should connect to.                                                                |
| Client Username | String | The client username. (See Notes below)                                                                                                    |
| Client Password | String | The client password. (See Notes below)                                                                                                    |

There are several ways you can get access to PubSub+ Messaging and find these required properties.

### Option 1: Use PubSub+ Cloud
Follow these instructions to quickly spin up a cloud-based PubSub+ messaging service for your applications.
The messaging connectivity information is found in the service details in the connectivity tab (shown below). You will need:

* Host:Port (use the SMF URI)
* Message VPN
* Client Username
* Client Password

![Connection Parameters Image](readmeImages/connectionParameters.png)

### Option 2: Start a PubSub+ Software
Follow [these instructions](https://docs.solace.com/Get-Started/Getting-Started-Try-Broker.htm?_ga=2.32239166.1891205303.1672824254-1972216927.1672824254&_gl=1*de5zvj*_ga*MTk3MjIxNjkyNy4xNjcyODI0MjU0*_ga_XZ3NWMM83E*MTY3MjgyNDI1My4xLjEuMTY3MjgyNDI2MS4wLjAuMA..) to start the PubSub+ Software in leading Clouds, Container Platforms or Hypervisors. The tutorials outline where to download and how to install the PubSub+ Software.
The messaging connectivity information are the following:

* Host: <public_ip> (IP address assigned to the VMR in tutorial instructions)
* Message VPN: default
* Client Username: sampleUser (can be any value)
* Client Password: samplePassword (can be any value)

Note: By default, the PubSub+ Software "default" message VPN has authentication disabled.

To inspect the messages being produced, subscribe to the configured topic (`solace/samples/java/direct/pub/*`)in the broker console's **Try-Me** tab :</br> ![Subscriber Connection Image](readmeImages/subscriberImage.png)
