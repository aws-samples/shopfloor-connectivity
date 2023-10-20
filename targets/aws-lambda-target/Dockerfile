ARG baseimage=amazoncorretto:19
FROM ${baseimage}

WORKDIR /sfc

ADD build/distributions/aws-lambda-target.tar.gz .

CMD ["./aws-lambda-target/bin/aws-lambda-target", "-envport", "PORT", "-info"]