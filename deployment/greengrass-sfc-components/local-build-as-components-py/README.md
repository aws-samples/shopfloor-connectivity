Greengrass components from local build
======================================

### Prerequisites
- aws account & accountID at hand
- awscli installed & configured
- an S3 Bucket
    - to create a new one use: `aws s3 mb s3://<YOUR-UNIQUE-BUCKET-NAME>`
- python 3 installed
- sfc gradle build finished (`./gradlew build` in repository root)
    - that command builds sfc and produces tar.gz files of all sfc modules in `build/distribution`
    - that build dir should look like that here...
        ```sh
        build/distribution: ls -ltr
        -rw-r--r--  1 user  group  62183905 Oct 24 10:10 modbus-tcp.tar.gz
        -rw-r--r--  1 user  group  62183043 Oct 24 10:10 mqtt.tar.gz
        -rw-r--r--  1 user  group  66161674 Oct 24 10:10 opcua.tar.gz
        -rw-r--r--  1 user  group  64462718 Oct 24 10:10 s7.tar.gz
        -rw-r--r--  1 user  group  62556323 Oct 24 10:10 snmp.tar.gz
        ...
        ```

### Steps

#### 1. Check if you can run the python script `buildSfcComponentRecipes.py`

```sh
python3 buildSfcComponentRecipes.py --help
usage: buildSfcComponentRecipes.py [-h] [-d BUILDDIR] [-n COMPBASENAME] [-v COMPVERSION] [-b S3SFCBUCKET] [-p COMPNAMEPREFIX] [-s COMPNAMESUFFIX] [-r REGION] [-a ACCOUNTID]
...
```

#### 2. Run python script with params

```sh
    python3 buildSfcComponentRecipes.py \
    --s3sfcBucket s3://<YOUR-S3-BUCKET> \ # s3 bucket
    --buildDir ../../../build/distribution \ # that is the relative path to the `build/distribution` directory
    --compVersion 0.0.2 \ # component version
    --accountId 123456789 \ # your aws accountID
    --region eu-central-1 \ # region you want sfc-components to be deployed

    buildDir = ../../../build/distribution
    compBaseName = com.amazon.sfc
    compVersion = 0.0.2
    s3sfcBucket = s3://<YOUR-S3-BUCKET>
    compNamePrefix = latest
    compNameSuffix = latest
    region = eu-central-1
    accountId = 687795499488

    SFC Greengrass Recipes & Artifacts are ready locally!

    --> In order to install into your aws account run: ./install-sfc-components.sh
    --> In order to uninstall all sfc components run : ./delete-sfc-components.sh
```

- please check the created folder/file hierarchy in `build/distribution/<prefix>`

#### 3. Run the created shell script

    That script uses aws-cli to 1/ upload artifacts & recipes and 2/ register component recipes with Greengrass. The script should contain commands like that:

    ```sh
    aws s3 cp --recursive  ../../../build/distribution/latest s3://<YOUR-BUCKET>/latest --region eu-central-1
    aws greengrassv2 create-component-version  --inline-recipe fileb://../../../build/distribution/latest/recipes/com.amazon.sfc.aws-iot-analytics-target-0.0.2.json --region eu-central-1
    aws greengrassv2 create-component-version  --inline-recipe fileb://../../../build/distribution/latest/recipes/com.amazon.sfc.file-target-0.0.2.json --region eu-central-1
    ...
    ```

    Run it:

    ```sh
    ./install-sfc-components.sh
    ```

#### 4. Check AWS IoT Greengrass component listing

    Use aws-cli to get a list of registered component versions. You should see the sfc-components listed in the returned json.

    ```sh
    aws greengrassv2 list-components #| jq
    ```

#### 5. Next steps
Now create customized `in-process` or `IPC` SFC Greengrass Deployments as described here:

[**Link to sfc greengrass `in-process` howto docs**](../../docs/greengras-in-process/README.md)

[**Link to sfc greengrass `IPC` howto docs**](../../docs/greengras-ipc/README.md)


#### 6. Optional: Cleanup script
The script from [step 2](#2-run-python-script-with-params) also created an `undo` script, that you can use to delete all registered sfc-greengrass components:

```sh
./delete-sfc-components.sh
``` 
