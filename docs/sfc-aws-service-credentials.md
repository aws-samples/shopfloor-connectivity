# Session credentials for targets accessing AWS Services



Targets publishing their data to AWS services need credentials to get access to these services. Besides using the
standard chain credential (environment variables, credentials files) used by the (Java) AWS SDK, SFC has additional
support for using device certificates to obtain session credentials from
the [AWS IoT Credentials Provider Service](https://aws.amazon.com/blogs/security/how-to-eliminate-the-need-for-hardcoded-aws-credentials-in-devices-by-using-the-aws-iot-credentials-provider/).
Targets can refer to a client configuration that contains entries for the files with for the required device
certificate, private key and root CA certificate. SFC provides helpers, that can be used by the targets, to obtain
session credentials using these certificates and key files. These client configurations are in the
[AwsIotCredentialProviderClients](./core/aws-iot-credential-provider-configuration.md) section of the configuration file and are referred by the targets by setting the
CredentialProviderClient to an entry in that section. If the CredentialProviderClient is not set then SFC will fall back
on the default credentials provider chain as described [here](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html).

The SFC system incorporates logic for obtaining session credentials, ported from Greengrass V2, ensuring compatibility without dependency. This approach offers flexibility in certificate deployment, allowing manual deployment to the SFC-running device or utilization of Greengrass certificate management when available. The configuration provides a streamlined option to use Greengrass deployment certificate and key files without specifying individual file locations.

The SFC core includes certificate and key file content in the target configuration. Targets can use this information to obtain session credentials, leveraging SFC helper classes that cache session access key ID, secret access key, and session token, while managing token expiration and renewal.

For scenarios where targets run as IPC services on different devices than the SFC core, protecting configuration data transmission, including device certificates and private keys, is crucial. Two methods are available for this purpose:

- Encrypt all data exchanges between the SFC core and the target using TLS/SSL by specifying a certificate and key for the IPC server.
- Enable the CertificatesAndKeysByFileReference option in the client configuration. This setting instructs the SFC core to transmit only file paths rather than actual certificate and key content, requiring secure file access or physical deployment on the target device.

To accommodate targets that require internet access via proxy servers for obtaining session credentials or making AWS service calls, the client configuration can include proxy configuration information.

This comprehensive approach to credential management and secure communication ensures that SFC can operate efficiently and securely across various deployment scenarios and network configurations.

For more info see https://aws.amazon.com/blogs/security/how-to-eliminate-the-need-for-hardcoded-aws-credentials-in-devices-by-using-the-aws-iot-credentials-provider/



