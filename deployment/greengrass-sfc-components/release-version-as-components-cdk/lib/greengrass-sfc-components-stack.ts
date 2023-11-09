import * as cdk from 'aws-cdk-lib';
import { Construct } from 'constructs';
import { aws_s3 as s3 } from 'aws-cdk-lib';
import * as codebuild from "aws-cdk-lib/aws-codebuild";
import { Asset } from 'aws-cdk-lib/aws-s3-assets';
import * as path from 'path';
import * as iam from "aws-cdk-lib/aws-iam";


export class GreengrassSfcComponentsStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    const componentBucket = new s3.Bucket(this, 'sfc-gg-comps-bucket',{
      bucketName: 'sfc-gg-comps-'+this.account+'-'+this.region,
      objectOwnership: s3.ObjectOwnership.BUCKET_OWNER_ENFORCED,
      blockPublicAccess: s3.BlockPublicAccess.BLOCK_ALL,
      removalPolicy: cdk.RemovalPolicy.RETAIN
    })

    const fileAsset = new Asset(this, 'buildSfcComponentRecipes.py', {
      path: path.join(__dirname, "../../local-build-as-components-py")
    });

    const builder = new codebuild.Project(this, 'MyProject', {
      environment: {
        buildImage: codebuild.LinuxBuildImage.AMAZON_LINUX_2_5,
      },
      source: codebuild.Source.s3({
        bucket: fileAsset.bucket,
        path: fileAsset.s3ObjectKey
      }),
      buildSpec: codebuild.BuildSpec.fromObject({
        version: '0.2',
        env: {
          variables: {
            "AWS_REGION": this.region,
            "ACCOUNT_ID": this.account,
            "SFC_COMPONENT_BASENAME": "com.amazon.sfc",
            "SFC_COMPONENT_VERSION": "1.0.0",
            "SFC_COMPONENT_BUCKET": componentBucket.bucketName,
            "SFC_COMPONENT_PREFIX": "1.0.0",
            "SFC_LATEST_RELEASE_BUNDLE_URI": "https://dyy8lqvmsyeqk.cloudfront.net/efb9866/bundle/sfc-1.0.0.zip",
            "SFC_LATEST_RELEASE_BUNDLE_NAME": "sfc-1.0.0.zip"
          }
        },
        phases: {
          install: {
            runtime_versions: {
              python: 3.8
            },
            commands: [
              'ls -ltr',
              'yum install -y tree',
              'mkdir sfc-modules',
              'cd sfc-modules',
              'wget -q $SFC_LATEST_RELEASE_BUNDLE_URI',
              'unzip $SFC_LATEST_RELEASE_BUNDLE_NAME',
              'rm $SFC_LATEST_RELEASE_BUNDLE_NAME',
              'cd ..',
              'python3 buildSfcComponentRecipes.py --s3sfcBucket s3://$SFC_COMPONENT_BUCKET --buildDir sfc-modules --compVersion $SFC_COMPONENT_VERSION --compNamePrefix $SFC_COMPONENT_PREFIX --region $AWS_REGION --accountId $ACCOUNT_ID',
              'tree .',
              'cat install-sfc-components.sh',
              'cat delete-sfc-components.sh'
            ]
          },
          build: {
            commands: [
              'set -e',
              './install-sfc-components.sh'
            ]
          }
        }
      }),
    });

    const codeBuildCmd = new cdk.CfnOutput(this, '01sfcCodeBuildCommand', { value: "aws codebuild start-build --project-name "+builder.projectName });

    builder.addToRolePolicy(
      new iam.PolicyStatement({
        effect: iam.Effect.ALLOW,
        actions: [
          's3:PutObject', 
          's3:GetObject', 
          's3:GetObjectVersion', 
          's3:GetBucketAcl', 
          's3:GetBucketLocation',
          's3:CreateMultipartUpload',
          's3:ListMultipartUploadParts',
          's3:ListBucketMultipartUploads'
        ],
        resources: ["*"],
      })
    );
    builder.addToRolePolicy(
      new iam.PolicyStatement({
        effect: iam.Effect.ALLOW,
        actions: [
          'greengrass:*'
        ],
        resources: ["*"],
      })
    );

  }
  
  
}

