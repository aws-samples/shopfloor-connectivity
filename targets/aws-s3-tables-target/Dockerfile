ARG baseimage=amazoncorretto:19
FROM ${baseimage}

WORKDIR /sfc

ADD build/distributions/aws-s3tables-target.tar.gz .

CMD ["./aws-s3-target/bin/aws-s3tables-target", "-port", "PORT", "-info"]