ARG baseimage=amazoncorretto:19
FROM ${baseimage}

WORKDIR /sfc

ADD ./build/distributions/store-forward-target.tar.gz .

CMD ["./store-forward-target/bin/router-target",  "-envport", "PORT"]