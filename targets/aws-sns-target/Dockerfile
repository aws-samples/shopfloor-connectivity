ARG baseimage=amazoncorretto:19
FROM ${baseimage}

WORKDIR /sfc

ADD build/distributions/aws-sns-target.tar .

CMD ["./aws-sns-target/bin/aws-sns-target", "-envport", "PORT", "-info", "-info"]