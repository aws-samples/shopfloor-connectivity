ARG baseimage=amazoncorretto:19
FROM ${baseimage}

WORKDIR /sfc

ADD build/distributions/aws-s3-target.tar .

CMD ["./aws-s3-target/bin/aws-s3-target", "-port", "PORT", "-info"]