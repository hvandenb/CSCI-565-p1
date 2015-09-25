## Overview
Student: Henri van den Bulk
Class: CSCI-565

Demonstrates Socket Communication between Client and Server

## Design

The client and server communicate with each other by leveraging the sockets and a partial implementation of the STOMP protocol. This creates a peer-to-pear connection between the client and server.
The socket provides the ability to stream data, which is done in Java through the use Netty libraries. The Netty libraries are commonly used to develop custom protocols. The framework
is used in many contemporary designs and provides for high performance async capabilities.

The performance that Netty gains is by using the Event Loop concept. Instead of forking processes for each connection or communication channel it implements
an event loop approach. This is to share resources as much as possible. Both the client and the server have an event loop and process communications
in an async manner. This means that either side can perform other activities and hands off controll to the framework.

Communication is established through a logical Channel that has it's own buffer. The framework controls when the Bytes in the Buffer are send across
the wire. The client can flush the buffer to force the sending of the data. 
On the Server side it establishes a buffer as well that is the reciprocal of what the client has.

         +---------------------+                +---------------------+
Client   | Channel --> Buffer  | -----> Sever   | Buffer --> Channel  |
         +---------------------+                +---------------------+

When a client writes to the Channel it hands control to the framework and returns. It can get notifications from the framework when data was send or
if failures occurred. On the Server side it will invoke callbacks as well to notify that data was received.

# Server Design

The server is a single node that needs to be able to handle multiple clients that are connecting. Each connection will need to be independent as different
commands are issued. Also to avoid contention the server needs to be able to handle multiple connections. To handle this the server will have an event
loop that listens for new connection requests. When a connection is successful the server will create a handle which is managed inside the ServerHandler. 

The handler is then responsible for handling the communication with that specific client moving forward. The server needs to maintain a shared
state as the requirement is that send requests from the client are stored on the server in a First In First Out Manner (FIFO) queue.
This shared state in the server will ensure that when one client sends a message another client can receive it and if multiple message have been send then
they can be consumed in order. 

The FIFO is accomplished by using a single queue within the server, implemented using a thread safe Singleton. Given that multiple connections are working in different threads it's critical that
one client does not overwrite another. To accomplish this the Server leverages a ConcurrentLinkedQueue that implement sychnronozation points so that 
multiple threads don't impact the queue.

# Protocol

To ensure the command are handled correctly the design takes this into account by implementing a partial STOMP protocol. 
The protocol is good for simplicity and interoperability reasons. This is a simple protocol packages the data using frames.

COMMAND
header1:value1
header2:value2

Body^@

The COMMAND indicates the specific command that is being requested by the client. For this implementation we only have the following commands:
CONNECT - To indicate a message layer exchange and initiate a connection. 
SEND - To send a string of information, which is packed in the Body of the frame. 
SUBSCRIBE - Which subscribes the client to a given queue

The headers are used to provide additional meta data about the messages.

The protocol also has control flow where a message is ACKNOWLEDGED. Here are the flows:

Client --> Server
Client: CONNECT --> Server, Sever: CONNECTED --> Client
Cloent: SEND --> Server, Sever: RECEIPT --> Client
Client: SUBSCRIBE --> Server, Server: SUBSCRIBED --> Client
When a message is available:

Server: MESSAGE --> Client, Client: ACK


## Installation

The source code was developed using Java and the build tool Maven. It's important that the Maven build tool is install. To accomplish this the following steps need to be take:

* In the current directory execute the following command
./install.sh

This should produce a directory called **apache-maven-3.3.3** 

## Building

A seperate script is provided to build the solution.

* In the current directory execute the following command:

./build.sh

The build should be complete when the following message is displyed:

[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 45.863 s
[INFO] Finished at: 2015-09-03T23:25:15-06:00
[INFO] Final Memory: 18M/235M
[INFO] ------------------------------------------------------------------------


## Execution

To start the server the p1server command is used which accepts an argument of the port number on which it should listen. If no port number is provided the default 55555 will be used.

./p1server 55555

For the client the p1client command is used. This command has a number of arguments that can be used.

To send a file to the server use the following:

./p1client <server-name> <port-number> send <file-name>

Where:

server-name is the name of the machine name on which the server is running
port-number is the port on which the server is listening
file-name is the name of the file that needs to be send to the server

Example:

./p1client localhost 55555 send message1.txt

The client can also check with the server to see if a message is available that can be consumed by the client.

./p1client <server-name> <port-number> receive


