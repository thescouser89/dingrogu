# dingrogu

The application configures workflows to run on [Rex](https://github.com/project-ncl/rex). A particular workflow can consists of multiple Rex tasks interlinked together.

We want to have workflows for:
- repository creation (talking with [Repour](https://github.com/project-ncl/repour))
- milestone release
- build process

# Architecture
This application consists of 2 parts:
- The creation of the workflow to send to Rex
- An adapter part that translates Rex's `StartRequest` and `StopRequest` DTOs to the specific application (if necessary)

The adapter part might be necessary to not couple Rex's particular DTO requests with the specific downstream's
application API.

The project is configured to build a uber-jar by default.

## Workflow Creation
Rex requires that we specify for each task:
- an endpoint to start the request and its payload
- and endpoint to cancel the request and its payload
- mdc values

Rex then sends to the endpoint the `StartRequest` DTO which contains:
- positiveCallback
- negativeCallback
- payload
- mdc map
- taskResults map (in case a task needs the result of a dependant task)

# Rex Tutorial
When creating the tasks to send to Rex, we'll create a graph request containing tasks, and the dependencies between tasks.

The task uses the `Request` DTO to tell Rex:
- which service it needs to send the request
- the payload and headers and HTTP method

The `Request` we define in the graph request gets transformed into:
- `StartRequest`
- `StopRequest`

by Rex before sending the data to the service. The `Request` attachment becomes the `StartRequest` payload.

```mermaid
graph LR
    A(CreateGraphRequest generated from Grogu) --> Rex(Rex tasks)
    RexTask1 --> GroguAdapter(Grogu Adapter Endpoint) --> ActualService (Actual Service API)
    RexTask2 --> GroguAdapter(Grogu Adapter Endpoint) --> ActualService (Actual Service API)
```

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell script
./mvnw compile quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

## Packaging and running the application

The application can be packaged using:
```shell script
./mvnw package
```

## Creating a native executable

You can create a native executable using: 
```shell script
./mvnw package -Pnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: 
```shell script
./mvnw package -Pnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/dingrogu-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/maven-tooling.