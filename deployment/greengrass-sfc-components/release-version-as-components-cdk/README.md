Greengrass components CDK tooling
=================================

Use CDK to automatically install all SFC-modules as Greengrass components.

### Prerequisites
- aws account
- awscli installed & configured
- npm installed
- aws cdk installed and configured
- tsc installed


### 1. Bootstrap cdk into your account
```sh
cdk bootstrap
```

### 2. Build and cdk deploy

*NOTE*: the cdk stack, once created, emits an output called: `GreengrassSfcComponentsStack.01sfcCodeBuildCommand`
```sh
npm run build
cdk deploy --all
...
 ✅  GreengrassSfcComponentsStack

✨  Deployment time: 38.54s

Outputs:
GreengrassSfcComponentsStack.01sfcCodeBuildCommand = aws codebuild start-build --project-name MyProject39F7B0AE-<someID>
...
```

### 3. Trigger codebuild project using aws-cli

Use the command **that got emitted from the stack before**. **FYI:** Codebuild project(created by cdk before) downloads the latest release, creates recipes using the [python-script](../local-build-as-components-py/buildSfcComponentRecipes.py), uploads artifacts to S3 & registers components with AWS IOT Greengrass.

```sh
aws codebuild start-build --project-name MyProject39F7B0AE-<someID>
```

### 4. Check AWS IoT Greengrass component listing

Use aws-cli to get a list of registered component versions. You should see the sfc-components listed in the returned json.

```sh
aws greengrassv2 list-components #| jq
```

### 5. Next steps
Now create customized `in-process` or `IPC` SFC Greengrass Deployments as described here:

[**Link to sfc greengrass `in-process` howto docs**](../../../docs/greengrass-in-process/README.md)

[**Link to sfc greengrass `IPC` howto docs**](../../../docs/greengrass-ipc/README.md)

---

### Useful commands

* `npm run build`   compile typescript to js
* `npm run watch`   watch for changes and compile
* `npm run test`    perform the jest unit tests
* `cdk deploy`      deploy this stack to your default AWS account/region
* `cdk diff`        compare deployed stack with current state
* `cdk synth`       emits the synthesized CloudFormation template
* `cdk bootstrap`   bootstrap cdk into account
