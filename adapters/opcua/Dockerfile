ARG baseimage=amazoncorretto:19
FROM ${baseimage}

WORKDIR /sfc

ADD ./build/distributions/opcua.tar.gz .

CMD ["./opcua/bin/opcua",  "-envport", "PORT"]