# REST Protocol Adapter

The REST protocol adapter in SFC enables polling data from HTTP endpoints using GET requests, where the adapter periodically fetches data from configured REST APIs and transforms the JSON responses into the SFC's internal data format. The adapter supports query parameters and authentication for secure API access.

In order to use this adapter as in [in-process](../sfc-running-adapters.md#running-protocol-adapters-in-process) type adapter the type must be added to the [AdapterTypes](../core/sfc-configuration.md#adaptertypes) section in the [SFC configuration file](../core/sfc-configuration.md).

```json
"AdapterTypes" :{
  "REST" : {
    "JarFiles" : ["<location of deployment>/rest/lib"]
  },
  "FactoryClassName" : "com.amazonaws.sfc.rest.RestAdapter"
}
```



[**REST Adapter data mapping**](#rest-adapter-data-mapping)

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

The REST adapter retrieves data from a service via GET requests.

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
      "Request": "pumps",
      "Channels": {
        "Pumps": {
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

The REST adapter configuration defines the HTTP endpoints, authentication methods, polling intervals, and response mapping required to fetch data from REST APIs. Below are the configuration parameters needed to set up the REST adapter for data collection.

## RestSourceConfiguration

[SFC Configuration](../core/sfc-configuration.md) > [Sources](../core/sfc-configuration.md#sources) >  [Source](../core/source-configuration.md) 

The RestSourceConfiguration class extends [SourceConfiguration](../core/source-configuration.md), inheriting all base configuration properties while adding REST-specific fields

Source configuration for the REST protocol adapter. This type extends the [SourceConfiguration](../core/source-configuration.md) type.

- [Schema](#restsourceconfiguration-schema)
- [Examples](#restsourceconfiguration-examples)

**Properties:**
- [Channels](#channels)
- [Request](#request)
- [RestServer](#restserver)

---
### Channels
The channels property defines a list of data points to be extracted from REST query responses, where each channel maps to a specific value in the REST response. Channels can be temporarily disabled by prefixing their identifier with a "#" character, allowing for easy testing and troubleshooting without removing the channel configuration.

**Type**: Map[String,[RestChannelConfiguration](#restchannelconfiguration)

At least 1 channel must be configured.

---
### Request
The request property specifies the REST endpoint path to query, excluding the base URL (which is defined in the server configuration). It's a String value representing the relative path of the REST API endpoint. 

For instance, if you intend to access "https://api.restful-api.dev/objects/7", you would only specify "objects/7" in the request property, as the base URL is configured in the server section referred by the [RestServer](#restserver) property.

**Type**: String



---
### RestServer
The RestServer property specifies the identifier of the REST server configuration to use, which must match an existing server definition in the RestServers section of the REST adapter configuration. This String value links the source to its server configuration containing connection details like base URL and authentication parameters. The referenced server identifier must exist in the adapter configuration specified by the ProtocolAdapter attribute.

**Type**: String

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

The RestChannelConfiguration class extends  [ChannelConfiguration](../core/channel-configuration.md) , inheriting all base channel properties while adding REST-specific configuration to specify which elements to extract from the REST response. It defines how to map and transform specific data elements from the REST API response into SFC channel values, using the base ChannelConfiguration properties along with REST response parsing parameters.

- [Schema](#restchannelconfiguration-schema)
- [Examples](#restchannelconfiguration-examples)

**Properties:**
- [Json](#json)
- [Selector](#selector)

---
### Json
The Json property determines whether the REST response payload should be parsed as JSON format. It's a Boolean value that defaults to true, indicating that the response will be treated as JSON. Set this to false if the response is in a different format.


**Type**: Boolean


Default is true

---
### Selector
The Selector property defines a JMESPath expression to extract specific data from the JSON response payload. This String value allows you to navigate and filter complex JSON structures to retrieve the exact data point needed for the channel. If no Selector is specified, the entire response payload will be used as the channel value. The Selector can only be used when Json is true (default).

 For example, to extract a nested value like "data.readings.temperature" from a JSON response, you would specify this path as the Selector. This is particularly useful when dealing with complex JSON responses or when you need to extract specific elements from arrays or nested objects.

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

The RestAdapterConfiguration defines the configuration settings for REST servers that can be referenced by a source.  These server configurations contain the necessary connection details, authentication parameters, and other REST-specific settings that RestSources can use to connect to and retrieve data from REST endpoints

RestAdapterConfiguration extension the [AdapterConfiguration](../core/protocol-adapter-configuration.md) with properties for the REST Protocol adapter.

- [Schema](#restadapterconfiguration-schema)
- [Examples](#restadapterconfiguration-examples)

**Properties:**

- [RestServers](#restservers)



---
### RestServers
The RestServers property contains a collection of REST server configurations that can be used by REST sources. Each server configuration in this collection can be referenced by a [RestSource](#restsourceconfiguration) using the [RestServer](#restserver) attribute.

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

The RestServerConfiguration class defines the configuration parameters for a single REST server connection. It contains all the necessary settings to establish and maintain a connection to a REST endpoint.

This configuration can be referenced by multiple RestSources, allowing for reuse of common server settings across different data collection points.

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
The Headers property defines a collection of HTTP headers that will be included in every request sent to the REST server. These headers can specify things like: 

- Content type (e.g., "Content-Type: application/json")
- Authentication tokens (e.g., "Authorization: Bearer token123")
- Custom headers required by the API
- Accept types
- API keys

These headers are automatically added to each request made to the server, ensuring consistent header information across all communications with the REST endpoint.

**Type**: Map[String,String]


The header "Accept" is by default set to application/json.

---
### MaxRetries

The MaxRetries property specifies the maximum number of times the system will attempt to retry a failed REST request before giving up. This helps handle temporary network issues or brief server unavailability.

**Type**: Integer

Default value is 3.

---
### Port
The Port property specifies the port number that the REST server is listening on. It's an optional Integer value - if not specified, the system will use the default port number associated with the protocol being used (typically port 80 for HTTP or port 443 for HTTPS). This allows you to configure non-standard port numbers when needed, such as when the REST server is running on a custom port.

**Type**: Integer

---
### Proxy
The Proxy property allows you to configure proxy server settings when the client needs to access the REST server through a proxy. This is useful in environments where direct access to the REST server is not possible or when network security policies require traffic to go through a proxy server.

**Type**: [ClientProxyConfiguration](../core/client-proxy-configuration.md)

Optional

---
### RequestTimeout
The RequestTimeout property specifies the maximum amount of time (in milliseconds) that the client will wait for a response from the REST server before timing out the request. If the server doesn't respond within this time period, the request will be considered failed and may trigger a retry (depending on the [MaxRetries](#maxretries) setting)

**Type**: Integer

Default is 5000

---
### Server
The Server property defines the base URL or host address of the REST server as a String. It represents the root endpoint of the REST API that will be used for all requests.

- It should contain the base URL without specific endpoints or resource paths
- If the URL doesn't start with "http://" or "https://", the system automatically prepends "https://"
- It can include the domain and any base path that's common to all API endpoints

To retrieve an objects using requests as "https://api.restful-api.dev/objects/7", this would be "https://api.restful-api.dev"

**Type**: String


---
### WaitAfterReadError
The WaitAfterReadError property specifies the time duration (in milliseconds) that the system should wait before attempting another request after encountering a read error (a situation where all retry attempts have failed). This delay helps prevent overwhelming the server during error conditions and implements a basic back-off strategy.

**Type** : Integer

Default value is 1000.

---
### WaitBeforeRetry
The WaitBeforeRetry property defines the delay period (in milliseconds) between individual retry attempts when a request fails. This is different from [WaitAfterReadError](#waitafterreaderror), which specifies the wait time after all retries have failed (a read error).

Key aspects:

- Specifies how long to pause between each retry attempt
- Helps prevent overwhelming the server with rapid retry requests
- Works in conjunction with MaxRetries to control retry behavior

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



