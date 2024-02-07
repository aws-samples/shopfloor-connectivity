# HTTP API and UX for SFC - a Custom Config provider example

This example project creates a custom SFC config provider, capable of providing a local HTTP API and User Interface
running on a custom defined port. The aim here is to provide some blueprint and guidance for future custom-built User 
Interface Implementations for SFC.

The local API implementation allows to run CRUD operations on SFC configurations - a local file based DB for persistence
is auto-created on first startup. SFC configs can be crafted and manipulated in the UX-JSON Editor, selected, and fired 
against the running sfc-main process. Additionally, a custom SFC-LogWriter is used to redirect valuable SFC logs via 
websockets to the UX.

**Note**: That custom config provider is considered as an experimental implementation. Use only for testing & development 
of SFC configs.

### How to use the Config Provider - a working example

We will create step by step a working SFC environment with our UX Config Provider.

- Build the gradle project: 
    ```shell
    #cd examples/custom-api-ui-config-provider
    ../../gradlew build
    ```
- Prepare other needed SFC components and copy the built artifact
    ```shell
    # Define sfc version and directory
    export VERSION="1.0.7"
    export BUNDLE_URI="https://github.com/aws-samples/shopfloor-connectivity/releases/download/v$VERSION"
    export SFC_DEPLOYMENT_DIR="sfc"
    # Download and extract bundles into folder ./sfc
    mkdir $SFC_DEPLOYMENT_DIR && cd $SFC_DEPLOYMENT_DIR
    wget $BUNDLE_URI/{debug-target,opcua,sfc-main}.tar.gz
    cp ../build/distributions/custom-api-ui-config-provider.tar.gz .
    for file in *.tar.gz; do
      tar -xf "$file"
      rm "$file"
    done
    cd -
    ```
- Start sample docker container as an  OPC-UA data source
  ```shell
  docker run -d -p 4840:4840 ghcr.io/umati/sample-server:main
  ```

- Now have a look at the config file - [custom-api-ui-config-provider.json](custom-api-ui-config-provider.json)
  - Section `LogWriter`: a reference to the custom LogWriter implementation - send SFC logs as Websockets to the UX
  - Section `ConfigProvider`: the actual API & UX config provider and it's port configuration setting
  - the other sections in that file are typical SFC configs - in our case opc-ua Channel & Server config

  ```json
  {
    "AWSVersion": "2022-04-02",
    "Name": "Custom SFC API and UX Config Provider Example",
    "LogWriter": {
      "JarFiles": [
        "${SFC_DEPLOYMENT_DIR}/custom-api-ui-config-provider/lib"
      ],
      "FactoryClassName": "com.amazonaws.sfc.log.SocketLogWriter"
    },
    "ConfigProvider": {
      "Port": 5000,
      "JarFiles": [
        "${SFC_DEPLOYMENT_DIR}/custom-api-ui-config-provider/lib"
      ],
      "FactoryClassName": "com.amazonaws.sfc.config.CustomApiUiConfigProvider"
    },
  ...
  ...
    
  }
  ```
  
- Start sfc-main with the config file

  ```shell
  sfc/sfc-main/bin/sfc-main -config custom-api-ui-config-provider.json
  ```
  ```shell
  # see how SFC recognized the config provider
  2024-02-07 13:13:26.841 INFO  - Creating configuration provider of type ConfigProvider
  2024-02-07 13:13:26.858 INFO  - Waiting for configuration
  2024-02-07 13:13:26.864 INFO  - Sending initial configuration from file "custom-api-ui-config-provider.json"
  2024-02-07 13:13:27.323 INFO  - Using port 5000 from Config file
  2024-02-07 13:13:27.630 INFO  - Autoreload is disabled because the development mode is off.
  2024-02-07 13:13:27.730 INFO  - Application started in 0.379 seconds.
  2024-02-07 13:13:27.929 INFO  - Initializing ProtocolHandler ["http-nio-5000"], null
  2024-02-07 13:13:27.956 INFO  - Starting service [Tomcat], null
  2024-02-07 13:13:27.956 INFO  - Starting Servlet engine: [ApacheTomcat/9.0.83], null
  2024-02-07 13:13:28.139 INFO  - Starting ProtocolHandler ["http-nio-5000"], null
  # That is our localhost server starting
  2024-02-07 13:13:28.152 INFO  - Responding at http://0.0.0.0:5000
  2024-02-07 13:13:28.300 INFO  - {"AWSVersion":"2022-04-02","Name":"Custom SFC API and UX Config Provider Example","LogWriter":{"JarFiles":["${SFC_DEPLOYMENT_DIR}/custom-api-ui-config-provider/lib"],"FactoryClassName":"com.amazonaws.sfc.log.SocketLogWriter"},"ConfigProvider":{"Port":5000,"JarFiles":["${SFC_DEPLOYMENT_DIR}/custom-api-ui-config-provider/lib"],"FactoryClassName":"com.amazonaws.sfc.config.CustomApiUiConfigProvider"},"Version":1,"LogLevel":"Info","ElementNames":{"Value":"value","Timestamp":"timestamp","Metadata":"metadata"},"Schedules":[{"Name":"OPCUA-INIT-Schedule","Interval":500,"Description":"","Active":true,"TimestampLevel":"Both","Sources":{"OPCUA-SOURCE":["*"]},"Targets":["DebugTarget"]}],"Sources":{"OPCUA-SOURCE":{"Name":"OPCUA-SOURCE","ProtocolAdapter":"OPC-UA","AdapterOpcuaServer":"OPCUA-SERVER-1","Description":"OPCUA local test server","SourceReadingMode":"Polling","SubscribePublishingInterval":100,"Channels":{"ServerStatus":{"Name":"ServerStatus","NodeId":"ns=0;i=2256"},"ServerTime":{"Name":"ServerTime","NodeId":"ns=0;i=2256","Selector":"@.currentTime"},"State":{"Name":"State","NodeId":"ns=0;i=2259"},"Machine1AbsoluteErrorTime":{"Name":"AbsoluteErrorTime","NodeId":"ns=21;i=59048"},"Machine1AbsoluteLength":{"Name":"AbsoluteLength","NodeId":"ns=21;i=59066"},"Machine1AbsoluteMachineOffTime":{"Name":"AbsoluteMachineOffTime","NodeId":"ns=21;i=59041"},"Machine1AbsoluteMachineOnTime":{"Name":"AbsoluteMachineOnTime","NodeId":"ns=21;i=59050"},"Machine1AbsolutePiecesIn":{"Name":"AbsolutePiecesIn","NodeId":"ns=21;i=59068"},"Machine1FeedSpeed":{"Name":"FeedSpeed","NodeId":"ns=21;i=59039"}}}},"Targets":{"DebugTarget":{"Active":true,"TargetType":"DEBUG-TARGET"}},"TargetTypes":{"DEBUG-TARGET":{"JarFiles":["${SFC_DEPLOYMENT_DIR}/debug-target/lib"],"FactoryClassName":"com.amazonaws.sfc.debugtarget.DebugTargetWriter"}},"AdapterTypes":{"OPCUA":{"JarFiles":["${SFC_DEPLOYMENT_DIR}/opcua/lib"],"FactoryClassName":"com.amazonaws.sfc.opcua.OpcuaAdapter"}},"ProtocolAdapters":{"OPC-UA":{"AdapterType":"OPCUA","OpcuaServers":{"OPCUA-SERVER-1":{"Address":"opc.tcp://localhost","Path":"/","Port":4840,"ConnectTimeout":"10000","ReadBatchSize":500}}}}}
  2024-02-07 13:13:28.302 INFO  - Using port 5000 from Config file
  2024-02-07 13:13:28.521 INFO  - Received configuration data from config provider
  2024-02-07 13:13:28.522 INFO  - Waiting for configuration
  2024-02-07 13:13:28.523 INFO  - Creating and starting new service instance
  2024-02-07 13:13:28.537 INFO  - Adding websocket client!
  2024-02-07 13:13:29.096 INFO  - Eclipse Milo OPC UA Stack version: 0.5.1
  2024-02-07 13:13:29.096 INFO  - Eclipse Milo OPC UA Client SDK version: 0.5.1
  2024-02-07 13:13:29.699 INFO  - SecureRandom seeded in 0ms.
  # Logwriter redirects SFC logs via websockets to the UX
  2024-02-07 13:13:31.118 INFO  - Adding websocket client!
  ```

- Done! Now visit [localhost:5000](http://localhost:5000)

### User Interface

![SFC-Demo Run](./img/configUX.gif)

### HTTP API resources

| resource    | method | payload | returns | description                     |
|-------------|--------|---------|---------|---------------------------------|
| config      | GET    |         | []      | List all configs by id and name |
| config      | POST   | json    | Int     | Create new SFC config           |
| config/{id} | GET    |         | object  | get config by id                |
| config/{id} | PUT    | json    | object  | update config by id             |
| config/{id} | DELETE |         |         | delete config by id             |
| push/{id}   | POST   |         | object  | push config by id to SFC        |
| pushed      | GET    |         | Int     | get the current pushed config   |
| hostname    | GET    |         | json    | get hostname and IP             |