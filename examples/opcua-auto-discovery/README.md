# SFC Example OPCUA auto discovery custom config provider

This is an example of a custom config provider allowing auto discovery of nodes to generate channels for configures  OPCUA sources.

The input SFC configuration file used for sfc-main must the configuration for the OPCUA sources used by the OPCUA protocol 
adapter. Nodes on the OPCUA servers, which are configured for the sources, are browsed and for selected noded channels will
be generated and added to the source. The initial list of channels for a source can be left empty. If the source already does 
contain channels, the channels for the discovered nodes are added to the existing ones.

The auto discovery configuration provider uses the OPCUA server configurations of the OPCUA servers used by the sources for which auto 
discovery is configured to connect to the OPCUA servers. This includes the settings as the configured security policy and
X509 certificates which are used for that policy.

Auto discovery for a source is considered as failed if no nodes where discovered, due to errors connecting to the server or 
browsing or misconfigured auto discovery for that source. The provider can be configured to periodically retry the discovery with a configurable
retry interval and maximum number of retries.

After the provider finishes the auto discovery, it will remove all sources without channels from the configuration. 
If after removing these sources no sources are left, then configuration is considered as invalid and generation will fail.

As the configuration provider is dynamically loaded by the SFC Core process, the system on which this process executes
must have network access to the OPCUA server from which the nodes are retrieved.

To use the auto discovery provider it must be added as a custom configuration  provider to the SFC configuration as shown in the 
configuration snippet below. Note that besides the directory containing the provider, the directory containing the opcua 
adapter must be added as well, as it is used by the provider. This is even the case if the OPCUA adapter is included as an 
adapter type in the AdapterTypes section of the SFC configuration. (Adjust location of jar files to actual location 
of the deployment)

```json
{ 
    "AWSVersion": "2022-04-02",
    "Name": "Configuration using custom YAML configurations",
    "ConfigProvider": {
        "JarFiles": [
                     "examples/opcua-auto-discovery/build/libs",
                     "adapters/opcua/build/libs"
                    ],
        "FactoryClassName": "com.amazonaws.sfc.config.OpcuaAutoDiscoveryConfigProvider"
    }

```

The __AutoDiscovery__ configuration section contains the configuration for the auto discovery provider.


```json
 "AutoDiscovery": {
    "Sources": {
      "OPCUA-SOURCE": 
      [
        {
            "NodeId": "ns=3;s=85/0:Simulation",
            "DiscoveryDepth": "10",
            "DiscoveredNodeTypes": "VariablesAndEvents",
            "Exclusions": [ ".*Max\\sValue.*", ".*Min\\sValue.*"]
        },
        {
            "NodeId": "ns=0;i=2253",
            "DiscoveredNodeTypes": "VariablesAndEvents",
            "Exclusions": [ "ServerDiagnostics/.*"]
        },
        {
            "NodeId": "ns=6;s=MyDevice",
            "DiscoveredNodeTypes": "VariablesAndEvents",
            "Inclusions" : [".*/MyLevel.*"]
        }
      ]
    }
    "IncludeDescription": true,
    "WaitForRetry": 60000,
    "MaxRetries": 10,
    "SavedLastConfig" : "generated-config.json",
  },
```

The __Sources__ section contains a table with an entry for each source for which nodes will be recovered. The key in this
table must match the name an OPCUA source configured for the OPCUA protocol adapter. The sources are configured as "normal"
OPCUA sources with the exception that the source does have to contain any channels.
Each source entry does have a list of at least one, or more entries which specify from where and how the provider will browse
to discover the nodes. The provider will check if the source names do exist as actual opcua sources and if there are sources that
don't have channels configured do have auto discovery configured.

An entry includes:

- __NodeId__: From this node the provider will browse for sub nodes, this must be a valid and readable node on the server.
- __DiscoveryDepth__ (optional): This is the number of levels the provider wil browse down from the specified NodeId. By setting 
the value to 0, which is the default value, there will no maximum depth.
- __DiscoveredNodeTypes__ (optional): The types of nodes to discover. Values can be "Variables" to discover variable nodes only, 
"Events" to discover  event and alarm nodes only, or "VariablesAndEvents"(the default) for both types. In order to 
include event and alarm nodes for a source the type of the event node or alarm node must be known as a  name of the OPCUA alarms from 
the model at https://reference.opcfoundation.org/Core/Part9/v105/docs/5.8, an OPCUA event from the model at
https://reference.opcfoundation.org/Core/Part3/v104/docs/9.1 or a custom companion  specification event type configured 
in the profile (see OPCUA adapter documentation for details on how to configure server profiles with custom event types).
- __Inclusions__ (optional): This is a list of regular expressions which used to filter nodes. If this list is present 
the node path must at least match one expression from this list to be included. The node path is a concatenation of the browse names from the 
specified NodeId from which the provider browses down to, and including the tested node, concatenated with a '/' as separator. 
Note that browse names can include spaces (which must be specified as "\s" in the regex and that special characters in the 
regex like '\' must be escaped with an additional '\\' as to keep JSON syntax of the configuration valid.
- __Exclusions__ (optional): This is a list of regular expressions which will be used to filter nodes. If this list is present
   the node is excluded if an expression in the list matches the node path. (see Inclusions for the definition of a node path)

The provider configuration has the following additional optional settings:

- __IncludeDescription__ (optional): If set to true (the default) then if the server node has a description it will be used as description for 
the channel created for that node
- __WaitForRetry__ (optional): Time in milliseconds between retries if auto discovery for one or more sources fails. The default is 60000 ms.
- __MaxRetries__ (optional): Max number of retries, default is 10 retries. Set to 0 to disable retries.
- __SavedLastConfig__ (optional): This is the name of the file to which the last generated configuration will be saved. 
Note that if the file already exists it will be overwritten by the provider.

When the content of the file which is used as value for the -config parameter is updated, the provider will automatically run the discovery
process.

The file opcua-auto-discovery-config.json is included as an example and starting point for creating a configuration file leveraging the auto discover configuration provider.