ARG baseimage=amazoncorretto:19
FROM ${baseimage}

WORKDIR /sfc

ADD build/distributions/aws-s3-target.tar.gz .

CMD ["./aws-s3-target/bin/aws-s3-target", "-port", "PORT", "-info"]