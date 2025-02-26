
# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: MIT-0


ARG baseimage=amazoncorretto:19
FROM ${baseimage}

WORKDIR /sfc
ADD build/distributions/opcua-writer-target.tar.gz .
CMD ["./mqtt-target/bin/opcua-writer-target",  "-envport", "PORT", "-trace"]