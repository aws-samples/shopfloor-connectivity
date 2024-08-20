ARG baseimage=amazoncorretto:19
FROM ${baseimage}

WORKDIR /sfc
ADD build/distributions/sitewiseedge-target.tar.gz .
CMD ["./aws-sitewiseedge-target/bin/aws-sitewiseedge-target",  "-envport", "PORT", "-trace"]