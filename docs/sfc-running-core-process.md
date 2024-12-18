# Running the SFC core process

The main class for running the SFC core process is `com.amazonaws.sfc.MainController`. The build process creates a
sfc-main application in the sfc-main/build/distributions directory. The sfc-main.tar.gz file contains script files (*
*bin/sfc-main** and **bin/sfc-main.bat**) to launch the applications, and all required libraries (/lib/*.jar)

The main class for running the SFC core is `com.amazonaws.sfc.MainController`

The `sfc-main` application has the following command-line arguments:

| Parameter | Description |
| --- | --- |
| -config  | Name of SFC configuration file.  |
| -verify | Pathname of file containing the public key to verify the digital signature of the configuration passed to the sfc core by the config handler.  |
| -h, -help | Shows command line parameter help. |
| -error | Set log output level to error level. (Error message only) |
| -info | Set log output level to info level. (Info, warning and error messages) |
| -nocolor | Disable color coded output to console. |
| -trace | Set log output level to most detailed trace level (Info, warning, error, and detailed trace messages) |
| -warning | Set log output level to warning level. (Error and warning messages) |

