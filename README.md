# Shopfloor Connectivity Framework (SFC)

## WIP: Tenets for that README

- KISS
- in describing text - link to docs/README.md using markdown-refs
- Sequence could be:
  - What is SFC?
    - How does it fit into IDF & UNS patterns
  - How can I run it locally to test out things? (for HellowWorld use sfc artifacts from here: <https://dyy8lqvmsyeqk.cloudfront.net/>)
    - show off mock-up data sources like opcua/plcsim and pipe to IoT Core & Sitewise
    - create catchy screen-capture gifs showing sources & target raw data streams
  - How can I configure it?
  - How can I deploy it using Greengrass deployments?
  - How can I build it?
  - How can I write my own adapters?

## Introduction

Shop Floor Connectivity (SFC) is a data ingestion technology that can deliver data to multiple AWS Services.

SFC addresses limitations of, and unifies data collection of our existing IoT data collection services, allowing customers to collect data in a consistent way to any AWS Service, not just the AWS IoT Services, that can collect and process data. It allows customers to collect data from their industrial equipment and deliver it the AWS services that work best for their requirements. Customers get the cost and functional benefits of specific AWS services and save costs on licenses for additional connectivity products.

### SFC Components

There are three main type of components that make up SFC.

- Protocol Adapters
- SFC Core
- Target Adapters

<p align="center">
  <img src="docs/img/fig01.png" width="75%"/>
</p>
<p align="center">
    <em>Fig. 1. SFC components</em>
</p>

[Read more](docs/README.md/#introduction)

## Quick start - With Docker

## Quick start - With Executeables

## Configuration

## Build

## Deploy with Greengrass

## Write your own adapters
