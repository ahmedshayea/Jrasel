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

## Features
- **Client-Server Architecture**: A robust server that can handle multiple concurrent clients.
- **Custom Protocol**: A simple, text-based protocol for client-server communication.
- **Authentication**: Secure user authentication and signup.
- **Group Chat**: Users can create, join, and send messages to groups.
- **Multiple Clients**:
    - **Terminal Client**: A command-line interface for interacting with the chat server.
    - **GUI Client**: A user-friendly graphical interface built with Swing.
- **In-Memory Data Store**: The server uses an in-memory data store for users and groups, with a database-ready architecture.

## Project Structure

The project is organized into the following main packages:

-   `com.rasel.server`: Contains the server-side logic, including connection management, authentication, and request handling.
-   `com.rasel.client`: Contains the client-side logic, including the base `Client` class and the `TerminalClient`.
-   `com.rasel.gui`: Contains the Swing-based GUI client, including the `Launcher`, `LoginFrame`, and `ChatFrame`.
-   `com.rasel.common`: Contains classes shared between the client and server, such as the `Request` and `Response` builders and parsers.
-   `com.rasel.server.db`: Contains the in-memory database implementation for managing users and groups.

## How It Works

### Server

1.  The `Server` class initializes a `ServerSocket` and listens for incoming client connections.
2.  For each new connection, a `ClientHandler` thread is created to handle communication with that client.
3.  The `ClientHandler` reads requests from the client, parses them using `RequestParser`, and processes them based on the `RequestIntent`.
4.  The `AuthenticationManager` handles user authentication and signup, interacting with the `DatabaseManager`.
5.  The `ConnectionManager` keeps track of all connected clients and their authentication status.
6.  The `DatabaseManager`, along with `UserManager` and `GroupManager`, manages the application's data in memory.

### Client

1.  The `Client` class establishes a connection to the server and provides methods for sending requests and receiving responses.
2.  The `TerminalClient` provides a command-line interface for users to interact with the chat service.
3.  The `GuiClient` and the other classes in the `com.rasel.gui` package provide a graphical user interface for a more user-friendly experience.
4.  Both clients use the `RequestBuilder` to construct requests and the `ResponseParser` to interpret responses from the server.

## Protocol

The communication between the client and server is based on a custom, text-based protocol. Requests and responses are formatted as a series of key-value pairs, terminated by a special end-of-message marker.

### Request Format

```
INTENT:<intent>
CREDENTIALS:<username>:<password>
GROUP:<group_name>
DATA:<message>
END_OF_REQUEST
```

### Response Format

```
STATUS:<status>
DATA_TYPE:<type>
GROUP:<group_name>
DATA:<response_data>
END_OF_RESPONSE
```
