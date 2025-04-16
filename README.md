[<img src="./images/logo.png" width="300" height="150"/>](logo.png)

[![Maven Build & Sonar Analysis](https://github.com/eclipse-ecsp/api-gateway/actions/workflows/maven-build.yml/badge.svg)](https://github.com/eclipse-ecsp/api-gateway/actions/workflows/maven-build.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=eclipse-ecsp_api-gateway&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=eclipse-ecsp_api-gateway)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=eclipse-ecsp_api-gateway&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=eclipse-ecsp_api-gateway)
[![License Compliance](https://github.com/eclipse-ecsp/api-gateway/actions/workflows/licence-compliance.yaml/badge.svg)](https://github.com/eclipse-ecsp/api-gateway/actions/workflows/licence-compliance.yaml)
[![Latest Release](https://img.shields.io/github/v/release/eclipse-ecsp/api-gateway?sort=semver)](https://github.com/eclipse-ecsp/api-gateway/releases)

# Api Gateway

Api Gateway3 does provide single entry point for all clients to allow access of backend APIs. The API gateway handles
requests in one of two ways. Some requests are simply proxied/routed to the appropriate service. It handles other
requests by fanning out to multiple services. Spring Cloud API gateway, also provide a one-size-fits-all style API, the
API gateway can expose a different API for each client.

# Table of Contents

* [Getting Started](#getting-started)
* [Architecture](#architecture)
* [Usage](#usage)
* [How to contribute](#how-to-contribute)
* [Built with Dependencies](#built-with-dependencies)
* [Code of Conduct](#code-of-conduct)
* [Contributors](#contributors)
* [Security Contact Information](#security-contact-information)
* [Support](#support)
* [Troubleshooting](#troubleshooting)
* [License](#license)
* [Announcements](#announcements)
* [Acknowledgments](#acknowledgments)

## Getting Started

Spring Cloud Gateway provides 2 ways to configure the routes with gateway.

* Static Routing - User has to manually configure the route definitions in the application.yml
* Dynamic Routing - Api-registry-common has the capability to read the open api annotations mentioned on the endpoints
  and creates the route definitions and stores in postgressql database

To build the project in the local working directory after the project has been cloned/forked, run:

```mvn clean install```

from the command line interface.

### Prerequisites

1. The list of tools required to build and run the project:
   JDK 11/Java 17
   Apache Maven 3.8

Download and Install Apache Maven 3.8 locally and follow the below steps for IntelliJ IDE:

* Go to the right corner of the IDE and get inside the Maven option.
* Select the Maven Settings option and set the Maven Home Path to the path of the Apache Maven 3.8 installed locally.
* Click Apply and then OK.

2. PostgreSQL or MongoDB need to installed and ignite database/schema should be created with the required
   tables/collection.

* Link of the database and table scripts to be installed :

### Installation

[How to set up Maven](https://maven.apache.org/install.html)

[Install Java](https://www.tutorials24x7.com/java/how-to-install-openjdk-17-on-windows)

### Coding style check configuration

[checkstyle.xml](./checkstyle.xml) is the coding standard to follow while writing new/updating existing code.

Checkstyle plugin [maven-checkstyle-plugin:3.2.1](https://maven.apache.org/plugins/maven-checkstyle-plugin/) is
integrated in [pom.xml](./pom.xml) which runs in the `validate` phase and `check` goal of the maven lifecycle and fails
the build if there are any checkstyle errors in the project.

To run the checkstyle plugin, run the below maven command.

```mvn checkstyle:check```

### Running the tests

To run the tests for this system run the below maven command.

```mvn test```

Or run a specific test

```mvn test -Dtest="TheFirstUnitTest"```

To run a method from within a test

```mvn test -Dtest="TheSecondUnitTest#whenTestCase2_thenPrintTest2_1"```

### Deployment

We can deploy this component as a Kubernetes pod by installing api-gateway and api-registry charts.

Link:
[Charts](../../../csp-opensource-charts/tree/main/api-gateway-spring)

## Architecture

Sequence diagram of api-gateway-spring:

[<img src="./images/apiGatewaySpringSequenceDiagram.png" width="800" height="300"/>](apiGatewaySpringSequenceDiagram.png)

## Usage

1. API-Gateway provides a robust solution for managing and routing HTTP request in microservice architecture. It
   dynamically registers routes and applies filters to validate requests and responses. Also offers built-in resilience
   and monitoring features to ensure scalable and reliable API management.

2. Spring Cloud API gateway does provide single entry point for all clients to allow access of backend APIs. The API
   gateway handles requests in one of two ways. Some requests are simply proxied/routed to the appropriate service. It
   handles other requests by fanning out to multiple services. Spring Cloud API gateway, also provide a
   one-size-fits-all style API, the API gateway can expose a different API for each client.

3. The API gateway also implement security, e.g. verify that the client is authorized to perform the request by
   validating the Oath2 token(JWT).A variation of this pattern is the Backends for frontends pattern. It defines a
   separate API gateway for each kind of client.

4. Spring Cloud Gateway features:

* Built on Spring Framework 5, Project Reactor and Spring Boot 2.0
* Able to match routes on any request attribute.
* Predicates and filters are specific to routes.
* Circuit Breaker integration.
* Spring Cloud Discovery Client integration
* Easy to write Predicates and Filters
* Request Rate Limiting
* Path Rewriting
* Response Caching

## Built With Dependencies

* [Spring](https://spring.io/projects/spring-framework) - The web framework used
* [Maven](https://maven.apache.org/) - Dependency Management
* [PostgreSQL](https://jdbc.postgresql.org/) - PostgreSQL driver
* [MongoDB](https://www.mongodb.com/docs/drivers/java-drivers/) - MongoDB Java driver
* [Project Lombok](https://projectlombok.org/) - Builder
* [Apache Common](https://commons.apache.org/proper/commons-lang/) - Java Library
* [Jackson](https://github.com/FasterXML) - Reading JSON Objects
* [Logback](https://logback.qos.ch/) - Log Functionality
* [slf4j](https://www.slf4j.org/) - Log Functionality
* [Mockito](https://site.mockito.org/) - Mocking Functionality
* [JUnit5](https://junit.org/) - Testing Framework) - Testing Framework
* [SpringCloud](https://spring.io/projects/spring-cloud) - Spring Cloud Gateway
* [Lombok](https://projectlombok.org/) - Lombok framework
* [Redis](https://redis.io/docs/latest/develop/use/client-side-caching/) - Redis In Memory Database
* [Spring doc Open-Api](https://springdoc.org/) - To generate the API Documentation

## How to contribute

Please read [CONTRIBUTING.md](./CONTRIBUTING.md) for details on our contribution guidelines, and the process for
submitting pull requests to us.

## Code of Conduct

Please read [CODE_OF_CONDUCT.md](./CODE_OF_CONDUCT.md) for details on our code of conduct, and the process for
submitting pull requests to us.

## Contributors

* **[Abhishek Kumar](https://github.com/abhishekkumar-harman)** - *Initial work*

The list of [contributors](../../graphs/contributors) who participated in this project.

## Security Contact Information

Please read [SECURITY.md](./SECURITY.md) to raise any security related issues.

## Support

Contact the project developers via the project's "dev" list - https://accounts.eclipse.org/mailing-list/ecsp-dev

## Troubleshooting

Please read [CONTRIBUTING.md](./CONTRIBUTING.md) for details on how to raise an issue and submit a pull request to us.

## License

This project is licensed under the Apache-2.0 License - see the [LICENSE](./LICENSE) file for details

## Announcements

All updates to this component are present in our [releases page](../../releases).
For the versions available, see the [tags on this repository](../../tags).