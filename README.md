# Jrasel

Jrasel is a client-server chat application built in Java. It features a custom, TCP-based protocol for real-time communication. The project includes a server that handles multiple clients, authentication, and group messaging, along with two client implementations: a terminal-based client and a graphical user interface (GUI) client built with Swing.

## Features

- **Client-Server Architecture**: A robust server that can handle multiple concurrent clients.
- **Custom Protocol**: A simple, text-based protocol for client-server communication.
- **Authentication**: Secure user authentication and signup.
- **Group Chat**: Users can create, join, and send messages to groups.
- **Multiple Clients**:
    - **Terminal Client**: A command-line interface for interacting with the chat server.
    - **GUI Client**: A user-friendly graphical interface built with Swing.
- **In-Memory Data Store**: The server uses an in-memory data store for users and groups, with a database-ready architecture.

## Class Diagram

The following diagram illustrates the high-level architecture of the Jrasel project:

```mermaid
classDiagram
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
        +handleRequest(RequestParser)
    }

    class ConnectionManager {
        -ServerSocket serverSocket
        -List<ClientHandler> clients
        -Map<String, ClientHandler> authenticatedClients
        +addClient(ClientHandler)
        +removeClient(ClientHandler)
        +addAuthenticatedClient(String, ClientHandler)
        +getClientHandlerByUserId(String)
    }

    class AuthenticationManager {
        -DatabaseManager db
        +authenticate(String, String)
        +signup(String, String)
    }

    class DatabaseManager {
        +UserManager userManager
        +GroupManager groupManager
    }

    class UserManager {
        -ArrayList<User> users
        +createUser(String, String)
        +getUser(String)
    }

    class GroupManager {
        -ArrayList<Group> groups
        +createGroup(String, User)
        +getGroup(String)
        +addMember(String, User)
    }

    class User {
        -String id
        -String username
        -String password
    }

    class Group {
        -String name
        -ArrayList<User> members
        -User admin
    }

    class Client {
        -String serverAddress
        -int serverPort
        -Socket socket
        -PrintWriter out
        -BufferedReader in
        -Credentials credentials
        +connect()
        +disconnect()
        +authenticate(Credentials)
        +signup(Credentials)
        +sendMessage(String, String)
    }

    class TerminalClient {
        -Client client
        +start()
    }

    class GuiClient {
        -ClientInterface client
        +getClient()
    }

    class Launcher {
        +main(String[])
    }

    class LoginFrame {
        -JTextField usernameField
        -JPasswordField passwordField
        +signIn()
    }

    class ChatFrame {
        -DefaultListModel<String> groupsModel
        -DefaultListModel<ChatMessage> messagesModel
        -JTextField messageField
        +loadGroups()
    }

    class RequestBuilder {
        -RequestIntent intent
        -Credentials credentials
        -String group
        -String data
        +getRequest()
    }

    class RequestParser {
        -RequestIntent intent
        -Credentials credentials
        -String group
        -String data
        +getIntent()
    }

    class ResponseBuilder {
        -ResponseStatus status
        -String group
        -DataType dataType
        -String data
        +getResponseString()
    }

    class ResponseParser {
        -ResponseStatus status
        -DataType dataType
        -String group
        -String data
        +getStatus()
    }

    Server --> ConnectionManager
    Server --> ClientHandler
    ClientHandler --> ConnectionManager
    ClientHandler --> AuthenticationManager
    ClientHandler --> RequestParser
    ClientHandler --> ResponseBuilder
    AuthenticationManager --> DatabaseManager
    DatabaseManager --> UserManager
    DatabaseManager --> GroupManager
    UserManager --> User
    GroupManager --> Group
    Group --> User
    Client --> RequestBuilder
    Client --> ResponseParser
    TerminalClient --> Client
    GuiClient --> Client
    Launcher --> GuiClient
    Launcher --> LoginFrame
    LoginFrame --> ChatFrame
    ChatFrame --> GuiClient
```

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

## How to Run

### Server

To run the server, execute the `main` method in the `com.rasel.Rasel` class.

### Terminal Client

To run the terminal client, execute the `main` method in the `com.rasel.client.TerminalClient` class.

### GUI Client

To run the GUI client, execute the `main` method in the `com.rasel.gui.Launcher` class.