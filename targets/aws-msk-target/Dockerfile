ARG baseimage=amazoncorretto:19
FROM ${baseimage}

WORKDIR /sfc

ADD build/distributions/aws-msk-target.tar.gz .

CMD ["./aws-msk-target/bin/aws-msk-target",  "-envport", "PORT", "-info"]