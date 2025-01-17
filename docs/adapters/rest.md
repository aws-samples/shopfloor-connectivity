# REST Protocol Adapter

- [REST Adapter data mapping](#rest-adapter-data-mapping)
  - [All object properties  a single channel value](#all-object-properties--a-single-channel-value)
  - [Object properties as separate channel values](#object-properties-as-separate-channel-values)
  - [Selecting object properties](#selecting-object-properties)
  - [Objects lists](#objects-lists)


**Configuration**:

- [REST Adapter Configuration](#rest-adapter-configuration)
- [RestSourceConfiguration](#restsourceconfiguration)
- [RestChannelConfiguration](#restchannelconfiguration)
- [RestAdapterConfiguration](#restadapterconfiguration)
- [RestServerConfiguration](#restserverconfiguration)



## REST Adapter data mapping

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

### All object properties  a single channel value

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

### Object properties as separate channel values

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

### Selecting object properties

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

### Objects lists

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



## REST Adapter Configuration



## RestSourceConfiguration

[SFC Configuration](../core/sfc-configuration.md) > [Sources](../core/sfc-configuration.md#sources) >  [Source](../core/source-configuration.md) 



Source configuration for the REST protocol adapter. This type extends the [SourceConfiguration](../core/source-configuration.md) type.

- [Schema](#restsourceconfiguration-schema)
- [Examples](#restsourceconfiguration-examples)

**Properties:**
- [Channels](#channels)
- [Request](#request)
- [RestServer](#restserver)

---
### Channels
The channels configuration for an REST source holds configuration data to read values from the result from a source REST query. 
"Commented" out by adding a "#" at the beginning of the identifier of that channel.

**Type**: Map[String,[RestChannelConfiguration](#restchannelconfiguration)

At least 1 channel must be configured.



---
### Request
This is the REST query that is executed to retrieve the values from the server.

**Type**: String

To retrieve an object from "https://api.restful-api.dev/objects/7", this would be "objects/7"

---
### RestServer
Rest Server Identifier for the REST server to read from. This referenced server must be present in the RestServers section 
of the adapter referred to by the ProtocolAdapter attribute of the source.

**Type**: String

Must be an identifier of a server in the RestServers section of the REST adapter used by the source.

### RestSourceConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "description": "Configuration for REST source",
  "allOf": [
    {
      "$ref": "#/definitions/SourceConfiguration"
    },
    {
      "type": "object",
      "properties": {
        "Channels": {
          "type": "object",
          "description": "Map of REST channel configurations",
          "additionalProperties": {
            "$ref": "#/definitions/RestChannelConfiguration"
          },
          "minProperties": 1
        },
        "Request": {
          "type": "string",
          "description": "Request path or endpoint"
        },
        "RestServer": {
          "type": "string",
          "description": "Reference to the REST server configuration"
        }
      },
      "required": ["Channels", "Request", "RestServer"]
    }
  ]
}

```

### RestSourceConfiguration Examples

```json
{
  "Name": "TemperatureSensor",
  "ProtocolAdapter" : "RestAdapter",
  "RestServer": "MainAPI",
  "Request": "/sensors/temperature",
  "Channels": {
    "CurrentTemp": {
      "Name": "CurrentTemp",
      "Json": true,
      "Selector": "@.temperature.current"
    },
    "Humidity": {
      "Name": "Humidity",
      "Json": true,
      "Selector": "@.humidity.value"
    }
  }
}

```

[^top](#rest-adapter-data-mapping)



## RestChannelConfiguration

[SFC Configuration](../core/sfc-configuration.md) > [Sources](../core/sfc-configuration.md#sources) > [Source](../core/source-configuration.md)  > [Channels](../core/source-configuration.md#channels) > [Channel](../core/channel-configuration.md)



The RestChannelConfiguration type extends the [ChannelConfiguration](../core/channel-configuration.md) class with channel properties for the REST protocol adapter.

- [Schema](#restchannelconfiguration-schema)
- [Examples](#restchannelconfiguration-examples)

**Properties:**
- [Json](#json)
- [Selector](#selector)

---
### Json
Indicates if the payload returned by the REST request is in Json format


**Type**: Boolean


Default is true

---
### Selector
A JMESpath query for selecting data from the returned payload of the REST request.
The selector can be used to select values from structured or list data types returned in the payload of the REST request.

**Type**: String

 JMESPath expression, see https://jmespath.org/
If no Selector is specified then the value for the channel will be the complete-returned payload of the query of its source, in which case
the data is the raw payload if the "Json" setting for the channel is set to false, or a JSON parsed value if "Json" is true.

A Selector can only be used if "Json" is set to true (the default).

### RestChannelConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "description": "Configuration for REST channel",
  "allOf": [
    {
      "$ref": "#/definitions/ChannelConfiguration"
    },
    {
      "type": "object",
      "properties": {
        "Json": {
          "type": "boolean",
          "description": "Indicates if the response should be parsed as JSON"
        },
        "Selector": {
          "type": "string",
          "description": "JSON path selector for extracting values from response"
        }
      }
    }
  ]
}

```

### RestChannelConfiguration Examples

```json
{
  "Name": "Temperature",
  "Json": true,
  "Selector": "@.sensor.temperature"
}

```

[^top](#rest-adapter-data-mapping)




## RestAdapterConfiguration

RestAdapterConfiguration extension the [AdapterConfiguration](../core/protocol-adapter-configuration.md) with properties for the REST Protocol adapter.

- [Schema](#restadapterconfiguration-schema)
- [Examples](#restadapterconfiguration-examples)

**Properties:**

- [RestServers](#restservers)



---
### RestServers
REST servers configured for this adapter. The REST source using the adapter must refer to one of these servers with the RestServer attribute.

**Type**: Map[String,[RestServerConfiguration](#restserverconfiguration)]

### RestAdapterConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "description": "Configuration for REST adapter",
  "properties": {
    "RestServers": {
      "type": "object",
      "description": "Map of REST server configurations",
      "additionalProperties": {
        "$ref": "#/definitions/RestServerConfiguration"
      },
      "minProperties": 1
    }
  },
  "required": ["RestServers"]
}

```

### RestAdapterConfiguration Examples

Here's the JSON Schema for RestAdapterConfiguration:

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "allOf": [
    {
      "$ref": "#/definitions/AdapterConfiguration"
    },
    {
      "type": "object",
      "properties": {
        "RestServers": {
          "type": "object",
          "description": "Map of REST server configurations",
          "additionalProperties": {
            "$ref": "#/definitions/RestServerConfiguration"
          },
          "minProperties": 1
        }
      },
      "required": [
        "RestServers"
      ]
    }
  ]
}
```



Single server configuration:

```json
{
  "RestServers": {
    "MainAPI": {
      "Server": "https://api.example.com",
      "Headers": {
        "Content-Type": "application/json",
        "Accept": "application/json"
      },
      "RequestTimeout": 5000
    }
  }
}
```



Example 2 - Multiple servers configuration:

```json
{
  "AdapterType" : "RestAdapterType",
  "RestServers": {
    "ProductionAPI": {
      "Server": "https://api.production.com",
      "Headers": {
        "Content-Type": "application/json",
        "Authorization": "${prod-token}"
      },
      "RequestTimeout": 10000
    },
    "MonitoringAPI": {
      "Server": "https://monitoring.production.com",
      "Headers": {
        "Content-Type": "application/json",
        "API-Key": "${key}"
      },
      "RequestTimeout": 5000
    }
  }
}
```




[^top](#rest-adapter-data-mapping)



## RestServerConfiguration

[RestAdapter](#restadapterconfiguration) > [RestServers](#restservers)



- [Schema](#restserverconfiguration-schema)
- [Examples](#restserverconfiguration-examples)

**Properties:**

- [Headers](#headers)
- [MaxRetries](#maxretries)
- [Port](#port)
- [Proxy](#proxy)
- [RequestTimeout](#requesttimeout)
- [Server](#server)
- [WaitAfterReadError](#waitafterreaderror)
- [WaitBeforeRetry](#waitbeforeretry)

---
### Headers
Headers for server requests.

**Type**: Map[String,String]


The header "Accept" is by default set to application/json.

---
### MaxRetries

**Type**: Integer

Maximum number of retries for reading from the REST server.

---
### Port
REST server port number

**Type**: Integer

Optional, if not specified then the port number for the used protocol is used.

---
### Proxy
Client Proxy configuration if the client is using a proxy server to access the REST server.

**Type**: [ClientProxyConfiguration](../core/client-proxy-configuration.md)

Optional

---
### RequestTimeout
Timeout in milliseconds for the server to return a result.

**Type**: Integer

Default is 5000

---
### Server
REST server host

**Type**: String



To retrieve an objects using requests as "https://api.restful-api.dev/objects/7", this would be "https://api.restful-api.dev"
If the server does not start with a  "http://" or "https:" protocol specification "https://" is used as default.


---
### WaitAfterReadError
Period in milliseconds to pause reading from the server after an error reading from that server.

**Type**: Integer

Default is 10000

---
### WaitBeforeRetry
Period in milliseconds to wait in between retires reading from the server.

**Type**: Integer

Default is 1000



### RestServerConfiguration Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "description": "Configuration for REST server",
  "properties": {
    "Headers": {
      "type": "object",
      "description": "HTTP headers to be included in requests",
      "additionalProperties": {
        "type": "string"
      },
      "minProperties": 1
    },
    "Password": {
      "type": "string",
      "description": "Password for authentication"
    },
    "Port": {
      "type": "integer",
      "description": "Server port number"
    },
    "Proxy": {
      "$ref": "#/definitions/ProxyConfiguration",
      "description": "Proxy configuration for the REST server"
    },
    "RequestTimeout": {
      "type": "integer",
      "description": "Timeout for REST requests in milliseconds"
    },
    "Server": {
      "type": "string",
      "description": "Server host address",
      "pattern": "^https?://.*"
    },
    "WaitAfterReadError": {
      "type": "integer",
      "description": "Wait time after read error in milliseconds"
    },
    "WaitBeforeRetry": {
      "type": "integer",
      "description": "Wait time before retry in milliseconds"
    }
  },
  "required": ["Server"]
}

```

### RestServerConfiguration Examples

Basic configuration:

```json
{
  "Server": "https://api.example.com",
  "Headers": {
    "Content-Type": "application/json",
    "Accept": "application/json"
  },
  "RequestTimeout": 5000
}
```



Secure configuration with authentication:

```json
{
  "Server": "https://secure-api.example.com",
  "Port": 443,
  "Headers": {
    "Content-Type": "application/json",
    "Accept": "application/json",
    "API-Key": "${key}",
    "Authorization": "${token}"
  },
  "MaxRetries" : 5,
  "RequestTimeout": 5000,
  "WaitBeforeRetry": 1000
}
```

[^top](#rest-adapter-data-mapping)



