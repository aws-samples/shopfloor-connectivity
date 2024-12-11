

# REST Adapter data mapping

The REST adapter fetches data from a service using GET requests.

A source within the adapter, configured to interact with the "PumpDataServer," utilizes the "pumps/1" request. 
This adapter uses a GET request to the URL "https://api.pumpserver.com/pumps/1" to retrieve the desired object which returns 
the following payload:

```
{"id":"1","name":"FluidConveyor-1","data":{"flow"64,"pressure":33}}
```

Source configuration (partial)

```json
    "REST-SOURCE": {
      "Name": "RestSource",
      "ProtocolAdapter": "REST",
      "RestServer": "PumpDataServer",
      "Request": "pumps/1"

```

Adapter configuration

```json
  "ProtocolAdapters": {
    "REST": {
      "AdapterType": "REST",
      "AdapterServer": "Pumps",
      "RestServers": {
        "PumpDataServer": {
          "Server": "https://api.pumpserver.com",
          "MaxRetries": 3,
          "WaitBeforeRetry": 1000,
          "WaitAfterReadError": 10000
        }
      }
    }
  }
```

For a source one or more channels must be defined. There are the following options:

## All object properties  a single channel value

The source configuration below has a single channel named "Object", without further channel configuration data. It's assumed that
the returned data is in JSON format. When the data is not JSON then the channel Configuration must include a setting `"Json" : false` 
in which case the raw data is used for the value of the channel.

```json
  "REST-SOURCE": {
    "Name": "RestSource",
    "ProtocolAdapter": "REST",
    "RestServer": "PumpDataServer",
    "Request": "objects",
    "Channels": {
      "Object": {}
    }
  }
```

This configuration results in the following output data

```json
    "timestamp": "2024-11-27T10:34:00.856635Z",
    "sources": {
      "RestSource": {
        "values": {
          "Object": {
            "value": {
              "id": "1",
              "name": "FluidConveyor-1",
              "data": {
                "flow" 64,
                "pressure": 33
              }
            }
          }
        },
        "timestamp": "2024-11-27T10:34:00.853744Z"
      }
    }
  }
```

## Object properties as separate channel values

The "Decompose" channel option can be used to create individual values for every element of a returned object.

```json
    "REST-SOURCE": {
      "Name": "RestSource",
      "ProtocolAdapter": "REST",
      "RestServer": "PumpDataServer",
      "Request": "pumps/1",
      "Channels": {
        "Object": {
          "Decompose" : true
        }
      }
    }
```
This results in the following output structure:

```json
{
  "schedule": "DEMO-DATA",
  "serial": "93702088-789c-4f38-91ff-e0aaea2c204d",
  "timestamp": "2024-11-27T11:11:15.825499Z",
  "sources": {
    "RestSource": {
      "values": {
        "Object.id": {
          "value": "1"
        },
        "Object.name": {
          "value": "FluidConveyor-1"
        },
        "Object.data.flow": {
          "value": 64
        },
        "Object.data.pressure": {
          "value": 33
        }
      },
      "timestamp": "2024-11-27T11:11:15.819938Z"
    }
  }
}

```

## Selecting object properties

By defining channels with a "Selector" object properties can be selected as the value for these channels. A selector
is a <a href="https://jmespath.org/"> JMESPath</a> query to select the data from the returned object. Having individual
channels also enable the option to apply transformation and filters on the selected values and ad channel level metadata.

The source configuration below has 4 channels with a selector to query the data

```json
    "REST-SOURCE": {
      "Name": "RestSource",
      "ProtocolAdapter": "REST",
      "RestServer": "PumpDataServer",
      "Request": "pumps/1",
      "Channels": {
        "Id": {
          "Selector": "id"
        },
        "Name": {
          "Selector": "name"
        },
        "flow" {
          "Selector": "data.flow"
        },
        "pressure" {
           "Selector": "data.pressure"
        }
      }
    }
```

The structure of the output data is for this configuration is:

```json
{
    "schedule": "DEMO-DATA",
    "serial": "39cf5c74-9bfa-46bf-a260-5d465d67c95e",
    "timestamp": "2024-11-27T10:27:05.285881Z",
    "sources": {
      "RestSource": {
        "values": {
          "Id": {
            "value": "1"
          },
          "Name": {
            "value": "FluidConveyor-1"
          },
          "flow" {
            "value": 64
          },
          "pressure" {
            "value": 33
          }
        },
        "timestamp": "2024-11-27T10:27:03.438145Z"
      }
    }
  }
```

## Objects lists

When a request returns a list of objects, then these values can be returned as a single channel value. Here a request "objects" is
used with a single channel named "Objects".

```json
"REST-SOURCE": {
      "Name": "RestSource",
      "ProtocolAdapter": "REST",
      "RestServer": "PumpDataServer",
      "Request": "pumps,
      "Channels": {
        "Pumps": {
        }
      }
    }
  }
```

The output is:

```json
{
  "schedule": "DEMO-DATA",
  "serial": "9e5de241-bcd7-4292-9c4e-0df5680561d0",
  "timestamp": "2024-11-27T10:43:23.830073Z",
  "sources": {
    "RestSource": {
      "values": {
        "Pumps": {
          "value": [
            {"id":"1","name":"FluidConveyor-1","data":{"flow"64,"pressure":33}},
            {"id":"2","name":"FluidConveyor-2","data":{"flow"68,"pressure":42}},
            {"id":"3","name":"FluidConveyor-3","data":{"flow"67,"pressure":17}},
          ]
        }
      },
      "timestamp": "2024-11-27T10:43:23.826884Z"
    }
  }
}
```

If the number of returned object is known, channels can be defined each selecting a value from the list.

```json
  "Sources": {
    "REST-SOURCE": {
      "Name": "RestSource",
      "ProtocolAdapter": "REST",
      "RestServer": "PumpDataServer",
      "Request": "objects",
      "Channels": {
        "Pump1": {
          "Selector" : "[0]"
        },
        "Pump2": {
          "Selector" : "[1]"
        },
        "Pump3": {
          "Selector" : "[2]"
        }
      }
    }
  },
```


```json
{
  "schedule": "DEMO-DATA",
  "serial": "3eb61f25-23e8-40d7-99ee-14db6dad9b31",
  "timestamp": "2024-11-27T11:35:31.949222Z",
  "sources": {
    "RestSource": {
      "values": {
        "Pump1": {
          "value": {
            "id": "1",
            "name": "FluidConveyor-1",
            "data": {
              "flow" 64,
              "pressure": 33
            }
          }
        },
        "Pump2": {
          "value": {
            "id": "2",
            "name": "FluidConveyor-2",
            "data": {
              "flow" 46,
              "pressure": 23
            }
          }
        },
        "Pump3": {
          "value": {
            "id": "4",
            "name": "FluidConveyor-3",
            "data": {
              "flow" 47,
              "pressure": 26
            }
          }
        }
      },
      "timestamp": "2024-11-27T11:35:31.869553Z"
    }
  }
}
```

Or if the number of returned objects is unknown a single channel definition can be used with the "Spread" option set to true.

```json
  "REST-SOURCE": {
    "Name": "RestSource",
    "ProtocolAdapter": "REST",
    "RestServer": "PumpDataServer",
    "Request": "objects",
    "Channels": {
      "Pump": {
        "Spread": true
      }
    }
  }
```

This results in a numbered channel being created for every object in the returned list.

```json
{
  "schedule": "DEMO-DATA",
  "serial": "59085f28-3a5c-46ad-a9b3-4732fddb2ffb",
  "timestamp": "2024-11-27T10:41:18.228132Z",
  "sources": {
    "RestSource": {
      "values": {
        "Pump.0": {
          "value": {
            "id": "1",
            "name": "FluidConveyor-1",
            "data": {
              "flow" 64,
              "pressure": 33
            }
          },
          "timestamp": "2024-11-27T10:41:18.224438Z"
        },
        "Pump.1": {
          "value": {
            "id": "2",
            "name": "FluidConveyor-2",
            "data": {
              "flow" 68,
              "pressure": 26
            }
          },
          "timestamp": "2024-11-27T10:41:18.224438Z"
        },
        "Pump.2": {
          "value": {
            "id": "3",
            "name": "FluidConveyor-3",
            "data": {
              "flow" 46,
              "pressure": 54
            }
          },
          "timestamp": "2024-11-27T10:41:18.224438Z"
        }
  }
}
```


# REST Adapter Configuration

This section describes the configuration types for the Rest adapter and contains the extensions and specific
configuration types

- [RestSourceConfiguration](#restsourceconfiguration)
- [RestChannelConfiguration](#restchannelconfiguration)
- [RestAdapterConfiguration](#restadapterconfiguration)
- [RestServerConfiguration](#restserverconfiguration)
- [ClientProxyConfiguration](#clientproxyconfiguration)

[Protocol Adapters](./README.md)

## RestSourceConfiguration

The RestSourceConfiguration extends the common <a href="../core/source-configuration.md">Source configuration</a> with 
REST specific source configuration data. The AdapterType for the source must be set to **REST**.

<table>
<colgroup>
<col style="width: 19%" />
<col style="width: 27%" />
<col style="width: 28%" />
<col style="width: 24%" />
</colgroup>

<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td><strong>Comments</strong></td>
</tr>

<tr class="even">
<td>Channels</td>
<td>The channels configuration for an REST source holds configuration data to read values from the result from a source REST query. 
"Commented" out by adding a "#" at the beginning of the identifier of that channel.</td>
<td>Map[String,<a href="#restchannelconfiguration">RestChannelConfiguration</a>]</td>
<td>At least 1 channel must be configured.</td>
</tr>

<tr class="odd">
<td>RestServer</td>
<td>Rest Server Identifier for the REST server to read from. This referenced server must be present in the RestServers section 
of the adapter referred to by the ProtocolAdapter attribute of the source.</td>
<td>String</td>
<td>Must be an identifier of a server in the RestServers section of the REST adapter used by the source.</td>
</tr>

<tr class="even">
<td>Request</td>
<td>This is the REST query that is executed to retrieve the values from the server.</td>
<td colspan="1">String</td>
<td colspan="1">
To retrieve an object from "https://api.restful-api.dev/objects/7", this would be "objects/7"</td>
</tr>


</tbody>
</table>

[^top](#rest-adapter-configuration)

## RestChannelConfiguration

The RestChannelConfiguration extends the common Channel configuration with REST specific channel configuration data.
A RestSourceConfiguration must at least have one channel. If more than a single channel is configured, the selector for
each channel will be executed to select the value for that channel from the result of the query defined for the source.

<table>
<colgroup>
<col style="width: 19%" />
<col style="width: 27%" />
<col style="width: 28%" />
<col style="width: 24%" />
</colgroup>

<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td><strong>Comments</strong></td>
</tr>

<tr class="even">
<td>Selector</td>
<td><p>A <a href="https://jmespath.org/">JMESpath</a> query for selecting data from the returned payload of the REST request.</p>
<p>The selector can be used to select values from structured or list data types returned in the payload of the REST request.</p></td>
<td>String</td>
<td> <a href="https://jmespath.org/">JMESPath</a> expression, see https://jmespath.org/
If no Selector is specified then the value for the channel will be the complete-returned payload of the query of its source, in which case
the data is the raw payload if the "Json" setting for the channel is set to false, or a JSON parsed value if "Json" is true.

A Selector can only be used if "Json" is set to true (the default).
</td>
</tr>

<tr class="even">
<td>Json</td>
<td>Indicates if the payload returned by the REST request is in Json format
</td>
<td>Boolean</td>
<td>
Default is true
</td>
</tr>

</tbody>
</table>

[^top](#rest-adapter-configuration)

## RestAdapterConfiguration

The RestAdapterConfiguration extends the common adapter configuration with REST specific adapter configuration settings. 
The AdapterType to use for this adapter is <strong>"REST"</strong>.

<table>
<colgroup>
<col style="width: 19%" />
<col style="width: 27%" />
<col style="width: 28%" />
<col style="width: 24%" />
</colgroup>

<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td><strong>Comments</strong></td>
</tr>

<tr class="even">
<td>RestServers</td>
<td>REST servers configured for this adapter. The REST source using the adapter must refer to one of these servers with the RestServer attribute.</td>
<td>Map[String,<a href="#restserverconfiguration">RestServerConfiguration</a>]</td>
<td></td>
</tr>

</tbody>
</table>

[^top](#rest-adapter-configuration)

## RestServerConfiguration

Configuration data for connecting to and reading from source REST servers

<table>
<colgroup>
<col style="width: 19%" />
<col style="width: 27%" />
<col style="width: 28%" />
<col style="width: 24%" />
</colgroup>

<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td>Comments</td>
</tr>

<tr class="even">
<td>Server</td>
<td>REST server host</td>
<td>String</td>
<td>

To retrieve an objects using requests as "https://api.restful-api.dev/objects/7", this would be "https://api.restful-api.dev"
If the server does not start with a  "http://" or "https:" protocol specification "https://" is used as default.
</td>
</tr>

<tr class="odd">
<td>Port</td>
<td>REST server port number</td>
<td>Integer</td>
<td>Optional, if not specified then the port number for the used protocol is used.</td>
</tr>

<tr class="even">
<td>WaitAfterReadError</td>
<td>Period in milliseconds to pause reading from the server after an error reading from that server.</td>
<td>Integer</td>
<td>Default is 10000</td>
</tr>

<tr class="odd">
<td>RequestTimeout</td>
<td>Timeout in milliseconds for the server to return a result.</td>
<td>Integer</td>
<td>Default is 5000</td>
</tr>

<tr class="even">
<td>WaitBeforeRetry</td>
<td>Period in milliseconds to wait in between retires reading from the server.</td>
<td>Integer</td>
<td>Default is 1000</td>
</tr>

<tr class="odd">
<td>Password</td>
<td>MaxRetries password</td>
<td>Integer</td>
<td><strong>Maximum number of retries for reading from the REST server.</strong></td>
</tr>

<tr class="even">
<td>Headers</td>
<td>Headers for server requests.</td>
<td>Map[String,String]</td>
<td>
The header "Accept" is by default set to application/json.
</td>
</tr>

<tr class="odd">
<td>Proxy</td>
<td>Client Proxy configuration if the client is using a proxy server to access the REST server.</td>
<td>ClientConfiguration</td>
<td>Optional</td>
</tr>


</tbody>
</table>

## ClientProxyConfiguration

Proxy configuration settings

<table>
<colgroup>
<col style="width: 19%" />
<col style="width: 27%" />
<col style="width: 28%" />
<col style="width: 24%" />
</colgroup>

<tbody>
<tr class="odd">
<td><strong>Name</strong></td>
<td><strong>Description</strong></td>
<td><strong>Type</strong></td>
<td><strong>Comments</strong></td>

</tr>
<tr class="even">
<td>ProxyUrl</td>
<td>Url of the proxy server to use </td>
<td>String</td>
<td></td>

</tr>
<tr class="odd">
<td>ProxyUsername</td>
<td>Proxy server username</td>
<td>String</td>
<td>Optional, if specified then the ProxyPassword must be configured as well.

Username and password should not be included as clear text in the configuration. 
It is strongly recommended to use placeholders and use the SFC integration with the AWS secrets manager.</td>

</tr>
<tr class="even">
<td>ProxyPassword</td>
<td>Proxy server password</td>
<td>String</td>
<td>Optional, if specified then the ProxyUsername must be configured as well.

Username and password should not be included as clear text in the configuration. 
It is strongly recommended to use placeholders and use the SFC integration with the AWS secrets manager.</td>

</tr>
<tr class="odd">
<td>NoProxyAddresses</td>
<td>Comma-separated list of addresses for which can be accessed without using the proxy</td>
<td>String</td>
<td>Optional</td>

</tr>

</tbody>
</table>


[^top](#rest-adapter-configuration)