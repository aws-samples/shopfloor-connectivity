# .NET Core based protocol adapters

- [.NET Core based protocol adapters](#net-core-based-protocol-adapters)

- [Running the .NET Core protocol adapters as an IPC Service](#running-the-net-core-protocol-adapters-as-an-ipc-service)

- [Output logging format](#output-logging-format)

- [Implementing a .NET Core Protocol adapter](#implementing-a-net-core-protocol-adapter)

- [Service logging](#service-logging)

  

In situations where .NET libraries are used to implement a protocol adapter the SFC framework provides a subset of the
full of classes that are required to implement the adapter, in a consistent with the JVM implementation, way using C#.
As these adapters cannot be loaded into the SFC core process these are implemented as server providing an IPC service
for the SFC core to configure and read the data from the adapter.

This section describes the steps to implement such a server and the key differences with a JVM based adapter.



## Running the .NET Core protocol adapters as an IPC Service

| **Protocol** | **Application name** |
|--------------|----------------------|
| OPCDA        | opdua                |

The applications do have all the following command line parameters in common.

| Parameter       | Description                                                                                                                                                                                                                                                                                                                                                    |
|-----------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| --cert          | PKCS12 Certificate file to secure IPC (gRPC) traffic using SSL (optional). As the gRPC implementation for the .NET framework uses certificates in a pkcs12 format, these might have to be generated first. This can be done using the openssl tool. openssl pkcs12 -export -out certificate.pfx -inkey privateKey.key -in certificate.crt -certfile CACert.crt |
| --config        | Name of the configuration file. The only value used from the configuration file is the port number the process will listen on for IPC requests. The SFC core will send an initialization request to the service on this port with the configuration data for the service to initialize its communication with the source device.                               |
| --envport       | The name of the environment variable that contains the port number for the service to listen on for requests.                                                                                                                                                                                                                                                  |
| --help          | Shows command line parameter help.                                                                                                                                                                                                                                                                                                                             |
| --password      | Password for the PKCS12 certificate file.                                                                                                                                                                                                                                                                                                                      |
| --port          | port number for the service to listen on for requests.                                                                                                                                                                                                                                                                                                         |


The port number, used by the service, can be specified using different methods which are applied in the following order

- The value of the -port command line parameter

- The value of the environment variable specified by the -envport parameter

- From the configuration file, specified by the -config parameter, the port number for the server referred to in the
  ProtocolSource/Server element will be used

To protect the ICP traffic between the core and the adapter SSL can be used. For this, the --cert and if required the
--password parameter must be used to specify the pathname to the certificate and the key file.



## Output logging format

In order to integrate with the Microsoft logging extensions, the command line  
the parameters for logging (-trace, -info, -warning, -error) are not available for
the [.NET Core based adapter](https://docs.microsoft.com/en-us/dotnet/core/extensions/logging?tabs=command-line) implementations. Instead of these parameters the level of output logging is configured in the appsettings.json file.



## Implementing a .NET Core Protocol adapter

- Create a project for the adapter and reference the sfc-core and sfc-ipc projects

- Build an adapter for the protocol that that implements the SFC IProtocolAdapter interface

- Create the host for the adapter service by creating a class that inherits from ProtocolServiceMain.

  - In this class implement the abstract method named CreateAdapterService that:

  - Sets the ProtocolAdapterServiceImpl.CreateAdapter delegate to a method  
    that does create the instance of the adapter used by the gRPC service.

  - Returns an instance of the Service class passing the  
    ProtocolAdapterServiceImpl class as its type parameter.

- Implement the configuration types required for the adapter. When initializing the adapter using the InitializeAdapter
  service call the JSON configuration for the adapter is passed as JSON data. When the CreateAdapter method (see above)
  is called, an instance of the sfc ConfigReader is passed as a parameter. An instance of the configuration class,
  containing the deserialized data can be obtained by calling the readers GetConfig method, passing the type of the
  configuration class. This configuration data is used to create and initialize the adapter. The configuration class
  must inherit from the SFC BaseConfiguration class. The IValidate class can be implemented which will be called to
  execute the configuration validation  
  logic after reading the data from the JSON configuration data.

- Implement a static main method for the service class that creates a (singleton) instance of that class, and calls it's
  from ProtocolServiceMain inherited Run method to start the service.
  
- 

```c#
public sealed class OpcdaProtocolService : ProtocolServiceMain
    {
        // Singleton instance with lock
        private static OpcdaProtocolService? _instance;
        private static readonly object InstanceLock = new();

        private OpcdaProtocolService()
        {
        }

        // Override method that creates an instance of the Service that 
        // hosts the gRPC ProtocolAdapter service
        protected override Service<ProtocolAdapterServiceImpl> CreateAdapterService()
        {
            // First set the static property to function delegate that is used by the
            // ProtocolAdapterServiceImpl to
            // create the specific protocol adapter implementation it is using. 
            // This pattern is required die to how dotnet core creates instances of the
            // actual service passing the class, not an instance of the service
            // implementation
            ProtocolAdapterServiceImpl.CreateAdapter = 
                 delegate(ConfigReader reader, ServiceLogger logger)
            {
                var config = reader.GetConfig<OpcdaConfiguration>();
                return OpcdaAdapter.CreateInstance(config, logger);
            };
            return new Service<ProtocolAdapterServiceImpl>();
        }

        private static OpcdaProtocolService Instance
        {
            get
            {
                lock (InstanceLock)
                {
                    return _instance ??= new OpcdaProtocolService();
                }
            }
        }

        public static void Main()
        {
            Instance.Run();
        }
    }
```



## Service logging

In order to integrate with the [Microsoft logging extensions](https://docs.microsoft.com/en-us/dotnet/core/extensions/logging?tabs=command-line)  the command line parameters for logging (-trace, -info, -warning, -error) are not available for the .NET Core based adapter
implementations. The level of the logging output is configured in the appsettings.json file.