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
on the default credentials provider chain as
described [here](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html).

The logic for obtaining the session credentials is ported from Greengrass V2 into SFC and is fully compatible with, but
not dependent on Greengrass. Certificates can be deployed manually to the device running SFC, or in case Greengrass is
deployed on the same machine make use of the Greengrass certificate management and deployment functionality. The
configuration provides a shortcut option to specify that the certificate and key files of a Greengrass deployment on
that device can use, without the need to specify the location of each certificate or key file.

The SFC core will provide the content of the certificate and key files as part of the configuration to the targets. The
targets can use this content to obtain session credentials, using SFC helper classes that will cache the session access
key id, secret access key, and session token, and obtain a new session if it expires.

In scenarios where a target is running as an IPC service on a different device as the device running the SFC core the
configuration data, including the device certificate and private key, over the network, this data needs to be protected.
This can be done using the following methods:

- Protect all data exchanged between the SFC core and the target over the network by specifying a certificate and key
  for that IPC server. If these are used the traffic is encrypted using TLS/SSL.
- Per client configuration, there is the option to set the CertificatesAndKeysByFileReference option to true. When this
  option is set for a target the SFC core will not pass the content of the certificate and key files over the network,
  but only the configured paths for these files. This means that these files should either be accessible in a secure way
  from the device running the target or physically be deployed to that device, manually or using Greengrass certificate
  management.

As targets may need to access the internet over a proxy server, to obtain the session credentials as described above,
and to make the required AWS service calls, the client configuration referred by the target can also include proxy
configuration information.

For more info see https://aws.amazon.com/blogs/security/how-to-eliminate-the-need-for-hardcoded-aws-credentials-in-devices-by-using-the-aws-iot-credentials-provider/
