# Rasel ( راسل )
Rasel is a clinet-server real-time chat application built on top of **rasel** protocol, 
which is a custom, TCP-based protocol for real-time communication, this repo contains
both server and client implementations as well as a dedicated Terminal Based client
and a GUI client built with Swing.

## Motivation
Why should I spend months learning complex frameworks and protocols to build a simple real-time chat application?

I was trying to build a simple chat application and I found that I have to learn **Enterprise** level frameworks and protocols like **WebSocket**, just to build my simple chat app?

That seemed like using a sword to cut a paper!

So I decided to build my own real-time TCP-based protocol and a simple chat application on top of it.

## Running Applications

> [!IMPORTANT]
> This project uses Java 21 and Maven as build tool and dependency manager, make sure to have maven installed on your system, if it is not yet installed, you can follow the officila instructions [here](https://maven.apache.org/install.html).

This project hosts three main applications: 
1.  **Server**: The server application that handles client connections, authentication, and message routing.
2.  **Terminal Client**: A command-line interface for users to interact with the chat server.
3.  **GUI Client**: A graphical user interface built with Swing for a more user-friendly experience.

to run any of these applications, first you need to clone the repository: 

```bash
git clone https://github.com/ahmedshayea/Rasel.git
cd Rasel 
```

Then you can build the desired application using Maven, each application has its own Maven profile, available profiles are: `server`, `gui`, `terminal`

To build the server application, run:

```bash
mvn package -P server
```
This will generate a fat JAR file located at `target/rasel-server.jar` 

use the following command to run the jar file: 

```bash
java -jar target/rasel-server.jar
```

Other applications can be build the same way, just replace the profile name with the desired one: 

```bash
mvn package -P gui # for GUI client
mvn package -P terminal # for Terminal client 
```

To run the GUI client, use:

```bash
java -jar target/rasel-gui.jar
```
To run the Terminal client, use:

```bash
java -jar target/rasel-terminal.jar
```

> [!IMPORTANT]
> Make sure the server is running before starting any client application, gui client and terminal client will fail to start if the server is not running.

## Features
these are some of the features in this project:
- **Client-Server Architecture**: A robust server that can handle multiple concurrent clients.
- **Rasel Protocol**: A simple, text-based , real-time protocol.
- **Authentication**: Login and signup functionality.
- **Group Chat**: Create groups and add members to groups.
- **Multiple Clients**:
    - **Terminal Client**: A CLI interface for interacting with the chat server.
    - **GUI Client**: A user-friendly graphical interface built with Swing.
- **In-Memory Data Store**: A simple in-memory storage system with database ready architecutre.

## Class Diagram

```mermaid
classDiagram
    class Rasel {
        +main(String[] args)
    }

    class Server {
        -ServerSocket socket
        -ConnectionManager connectionManager
        +acceptConnections()
    }

    class ClientHandler {
        -Socket clientSocket
        -ConnectionManager connectionManager
        -User user
        -PrintWriter out
        -BufferedReader in
        -AuthenticationManager authManager
        +run()
        -handleRequest(RequestParser request)
    }

    class ConnectionManager {
        -ServerSocket serverSocket
        -List<ClientHandler> clients
        -Map<String, ClientHandler> authenticatedClients
        +addClient(ClientHandler client)
        +removeClient(ClientHandler client)
        +addAuthenticatedClient(String userId, ClientHandler client)
        +getClientHandlerByUserId(String userId)
    }

    class AuthenticationManager {
        +authenticate(String username, String password) User
        +signup(String username, String password) User
    }

    class Client {
        -String serverAddress
        -int serverPort
        -Socket socket
        -PrintWriter out
        -BufferedReader in
        -Credentials credentials
        -ResponseBus responseBus
        +connect()
        +disconnect()
        +authenticate(Credentials credentials)
        +sendMessage(String group, String message)
    }

    class ClientInterface {
        <<interface>>
        +connect()
        +disconnect()
        +authenticate(Credentials credentials)
        +sendMessage(String group, String message)
    }

    class TerminalClient {
        -ClientInterface client
        -Scanner scanner
        +start()
        -handleUserInput()
    }

    class GuiClient {
        -ClientInterface client
        +getClient() ClientInterface
    }

    class ResponseBus {
        -Map<ResponseResource, CopyOnWriteArrayList<Consumer<ResponseParser>>> listeners
        +on(ResponseResource resource, Consumer<ResponseParser> handler) AutoCloseable
        +publish(ResponseParser resp)
    }

    class RequestBuilder {
        -RequestIntent intent
        -Credentials credentials
        -String group
        -String data
        +getRequest() String
    }

    class ResponseBuilder {
        -ResponseStatus status
        -ResponseResource resource
        -String group
        -DataType dataType
        -String data
        +getResponseString() String
    }

    class RequestParser {
        -RequestIntent intent
        -Credentials credentials
        -String group
        -String data
        +getIntent() RequestIntent
    }

    class ResponseParser {
        -ResponseStatus status
        -DataType dataType
        -ResponseResource resource
        -String group
        -String data
        +getStatus() ResponseStatus
    }

    class DatabaseManager {
        +UserManager userManager
        +GroupManager groupManager
        +ChatMessageManager chatMessageManager
    }

    class UserManager {
        -ArrayList<User> users
        +createUser(String username, String password) User
        +getUser(String username) User
    }

    class GroupManager {
        -ArrayList<Group> groups
        +createGroup(String name, User admin) Group
        +getGroup(String name) Group
    }

    class ChatMessageManager {
        -ArrayList<ChatMessage> messages
        +addMessage(ChatMessage message)
        +getMessagesForGroup(Group group) ArrayList<ChatMessage>
    }

    class User {
        -String username
        -String password
        +checkPassword(String password) Boolean
    }

    class Group {
        -String name
        -ArrayList<User> members
        -User admin
        +addMember(User user)
        +isMember(User user) Boolean
    }

    class ChatMessage {
        -Group group
        -User sender
        -String content
        -String timestamp
    }

    Rasel --> Server
    Server --> ConnectionManager
    Server --> ClientHandler
    ClientHandler --> ConnectionManager
    ClientHandler --> AuthenticationManager
    ClientHandler --> RequestParser
    ClientHandler --> ResponseBuilder
    ClientHandler --> DatabaseManager
    ConnectionManager --> ClientHandler
    AuthenticationManager --> DatabaseManager
    Client --> ClientInterface
    Client --> ResponseBus
    Client --> RequestBuilder
    Client --> ResponseParser
    TerminalClient --> ClientInterface
    GuiClient --> ClientInterface
    GuiClient --> LoginFrame
    LoginFrame --> ChatFrame
    ChatFrame --> GuiClient
    DatabaseManager --> UserManager
    DatabaseManager --> GroupManager
    DatabaseManager --> ChatMessageManager
    UserManager --> User
    GroupManager --> Group
    GroupManager --> User

    Group --> User
    ChatMessageManager --> ChatMessage
    ChatMessage --> Group
    ChatMessage --> User

```

## Usage
In this section, I will provide an overview of how the project is strucutred, how to use it , and potentionally how to contribute to it if you want to.


### Project Structure

The project is splitted into the following main packages:

-   `com.rasel.server`: Contains the server-side logic, including connection management, authentication, and request handling.
-   `com.rasel.client`: Contains the client-side logic, including the base `Client` class ( which is the main client interface to interact with the rasel protocol) and the `TerminalClient`.
-   `com.rasel.gui`: Contains the Swing-based GUI client, including the `Launcher`, `LoginFrame`, and `ChatFrame`.
-   `com.rasel.common`: Contains classes shared between the client and server, such as the `Request` and `Response` builders and parsers.
-   `com.rasel.server.db`: Contains the in-memory database implementation for managing users and groups.

### How It Works

#### Server

1.  The `Server` class initializes a `ServerSocket` and listens for incoming client connections.
2.  For each new connection, a `ClientHandler` thread is created to handle communication with that client.
3.  The `ClientHandler` constantly reads requests from the client, parses them using `RequestParser`, and processes them based on the `INTENT`.
4.  The `AuthenticationManager` handles user authentication and signup, interacting with the `DatabaseManager`.
5.  The `ConnectionManager` keeps track of all connected clients and their authentication status.
6.  The `DatabaseManager`, along with `UserManager` ,`GroupManager`, and `ChatMessagesManager`, they manages application data, 
    currenltly in-memory but can be extended to use a persistent database without changing the classes interface implementation.

#### Client

1.  The `Client` class establishes a connection to the server and provides methods for sending various requests types and subscribes to various server messages, 
    it uses the `ResponseBus` to manage event-driven responses form the server, allow client to subscribe to specific response types ( response resources to be more accurate ).
2.  The `TerminalClient` provides a command-line interface for users to interact with the chat service, it uses the `Client` class to send and receive messages.
3.  The `GuiClient` and the other classes in the `com.rasel.gui` package provide a graphical user interface for a more user-friendly experience.
4.  Both clients use the `RequestBuilder` to construct requests and the `ResponseParser` to interpret responses from the server.

## Rasel Protocol Specifications

The specifications are still in development, this is the current version of the protocl:

### Request Format

A request is just a plain text string with the following format:

```
INTENT:<intent_name>
[CREDENTIALS:<username>:<password>]
[GROUP:<group_name>]
[DATA:<data>]
END_OF_REQUEST
```

-   `INTENT`: Specifies the purpose of the request. It's a mandatory field.
-   `CREDENTIALS`: Optional field for authentication. It includes the username and password separated by a colon.
-   `GROUP`: Optional field to specify a group identifier.
-   `DATA`: actual data of the request, think of it as the http body.
-   `END_OF_REQUEST`: Marks the end of the request.

### Request Intents

**INTENT** is a mandatory field, it is used by the server to determine how to handle the request, 
these are the available intents:

-   `AUTH`: Authenticate a user, performs a login.
-   `SEND`: Send a message to a group, group must be provided in the request.
-   `CREATE`: Create a new group.
-   `GET_GROUPS`: Get a list of all groups.
-   `GET_USERS`: Get a list of all users or users in a specific group, depends on the precentation of GROUP field, if you provided GROUP identifier, list of users in that group will be returned.
-   `ADD`: Add a user to a group, must provide the GROUP field.

#### Response Format

```
STATUS:<status>
RESOURCE:<resource_name>
DATA_TYPE:<type>
GROUP:<group_name>
DATA:<response_data>
END_OF_RESPONSE
```


## Limitations and Future Work
- **In-memory Storage:** ...
- **Encryption:**...

### Future Enhancements
- **Protocol Improvements:**...
- **Persistent Storage:**...
- **Enhanced GUI:**...
- **Additional Features:**...
