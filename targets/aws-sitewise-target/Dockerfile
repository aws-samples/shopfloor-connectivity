ARG baseimage=amazoncorretto:19
FROM ${baseimage}

WORKDIR /sfc

ADD build/distributions/aws-sitewise-target.tar .

CMD ["./aws-sitewise-target/bin/aws-sitewise-target", "-envport", "PORT", "-info", "-info"]