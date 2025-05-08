# Running the SFC core process

The main class for running the SFC core process is `com.amazonaws.sfc.MainController`. The build process creates a
sfc-main application in the sfc-main/build/distributions directory. The sfc-main.tar.gz file contains script files (*
*bin/sfc-main** and **bin/sfc-main.bat**) to launch the applications, and all required libraries (/lib/*.jar)

The main class for running the SFC core is `com.amazonaws.sfc.MainController`

The `sfc-main` application has the following command-line arguments:

| Parameter   | Description                                                                                                                                   |
|-------------|-----------------------------------------------------------------------------------------------------------------------------------------------|
| -config     | Name of SFC configuration file.                                                                                                               |
| -verify     | Pathname of file containing the public key to verify the digital signature of the configuration passed to the sfc core by the config handler. |
| -h, -help   | Shows command line parameter help.                                                                                                            |
| -error      | Set log output level to error level. (Error message only)                                                                                     |
| -info       | Set log output level to info level. (Info, warning and error messages)                                                                        |
| -nocolor    | Disable color coded output to console.                                                                                                        |
| -trace      | Set log output level to most detailed trace level (Info, warning, error, and detailed trace messages)                                         |
| -warning    | Set log output level to warning level. (Error and warning messages)                                                                           |

## Additional functionality to specify the config via environment variables

If you don't specify the `-config` parameter SFC will check the environment variable `SFC_CONFIG` if it exists and holds
a json configuration. This helps in environment where even default configuration is not passed as a file (e.g. in an AWS
IoT Greengrass component) 

## Running the process from a single jar file

The build process also creates an uber jar for SFC that contains the SFC core, all adaapters, targets, metrics and
examples in a single jar file. It is an executable jar that call the sfc-main application `com.amazonaws.sfc.MainController`.
This jar allow to 
* run the core process from a single executable e.g. `java - jar sfc-uberjar-1.x.x.jar -config xxx.json`
* load additional adapters, targets or other code included in the SFC project directly form that jar instead of loading it from different directories 

To load another component from the same jar (e.g. the same class path just omit the `JarFiles` entry in the configuration
for this component).
This example will load the debug target writer from the same classpath as the sfc core. As you can see there is no `JarFiles` specified. 
This assumes that all dependencies are included as well which is true for the uberjar.

```json
    "DEBUG-TARGET": {
      "FactoryClassName": "com.amazonaws.sfc.debugtarget.DebugTargetWriter"
    },
```

