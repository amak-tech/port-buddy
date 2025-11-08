# Project Guidelines

## Project description:
You are building a client-server application that exposes a port opened on the local host or in private network to the public network. It should be an analog of ngrok.com but much simpier. 

To expose a port, user run command-line application and specify host(optionally) and port number to expose. command template: `port-buddy [mode] [host:][port]`. Where:
* `mode` is optional and could be one of the following: [http, tcp]. `http` is a default mode.
* `host` is optional and could be valid ip address or domain name. For instance [localhost, 127.0.0.1, myserveraddress.com]. `localhost` is a default host.
* `port` is required. must be in range [1, 65535].

Http example: if user has a running web-app on the local machine at localhost:3000 and he wants to share it to someone else over the public network. User run command `port-buddy 3000`.
He gets back in the console a unique url to access the port in the public network. Console output: `http://localhost:3000 exposed to: https://random-subdomain.port-buddy.com`. 
If someone opens this url (https://random-subdomain.port-buddy.com) in the browser, he will see the web-app which is running on the user's local machine.
Technically, any http request or websocket connection to the url (https://random-subdomain.port-buddy.com) will be proxied through client application `port-buddy` to the user's web-app running at localhost:3000.

TCP example: user wants to open connection to the database which is running in private network to public network and share it to someone. For instance, user has postgresql db running on localhost:5432.
User run command `port-buddy tcp 5432`. Console output: `tcp localhost:5432 exposed to: tcp-proxy-3.port-buddy.com:43452`, where port 43452 is dynamically assigned based on port availability. 
Then someone could connect to user's database using database client by specifying host=random-subdomain.port-buddy.com and port=43452.

User must be authenticated and have active subscription. User could log into the system using Google or GitHub account. For authentication OAuth2 protocol with JWT tokens is used,  
To authenticate CLI client, user should generate API token in his account and then run command `port-buddy init {API_TOKEN}`, where {API_TOKEN} - is a placeholder for generated API token.

Resources provided to user are limited based on subscription plan. 
There are 3 subscription plans:
* basic
* individual
* professional

`basic` plan allows:
* http connections only
* up to 3Gb traffic per day
* cost: 5$

`individual` plan allows:
* everything in basic plan
* tcp connections
* up to 6Gb traffic per day
* cost: 10$

`professional` plan allows:
* everything in individual plan
* tcp connections
* up to 20Gb traffic per day
* cost: 20$


## Technical details: 

Project is written as multi-modular maven project and consists of the following modules:
* server application, serves API for cli app and web app, and handles all http requests and websocket connections. 
* tcp proxy application, handles tcp connections
* command-line app, which works as a proxy between client's private network and public network
* web application, which has landing page, user's app and admin app. 

Written in Java 25. 

### Java code style 
Google checkstyle rules are used. 
All variables which value is not changed must be marked with `final` modifier. 
All method params must be marked with `final` modifier.
For local variable `var` must be used instead of class name.
Lombok library is used for getters/setters, log reference, ect.
Do not shorten variable names. Always use meaningful names.
Use 4 spaces for indentation.
Must follow checkstyle rules: https://raw.githubusercontent.com/triel-dev/checkstyle/main/checkstyle.xml

### Server application
Is written using Spring Boot 3.5.7 framework. 
Any other necessary libraries could be used.
Uses PostgreSQL DB to store data.
Dockerized.

### TCP Proxy Application
Is written using Spring Boot 3.5.7 framework.
Any other necessary libraries could be used.
Dockerized.

### Command-line application
Is written without massive frameworks like Spring or Spring Boot. 
PicoCLI library should be used to handle cli arguments.
Could be built as a native app using GraalVM.

### Web application 
Is written in TypeScript using React framework and tailwindcss as a single page app.
All pages must be linked between each other. CEO optimised.
Has modern and stylish design. Font-family: "JetBrains Mono" or monospace.
Take the following web-sites as an example how it should look like:
* https://www.jetbrains.com/
* https://localxpose.io/

#### Index page.
Is a landing page that present project to a user in the best positive way.
Should have the following details:
* project description
* usage examples
* main functionality description
* use cases
* pricing

#### Installation page.
Here user should get an instructions how to install cli client app on different platforms like Windows, Linux, Mac.

#### App page.
Here user could manage subscription and see stats about resources he used. 
