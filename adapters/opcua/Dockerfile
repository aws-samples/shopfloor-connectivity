ARG baseimage=amazoncorretto:19
FROM ${baseimage}

WORKDIR /sfc

ADD ./build/distributions/opcua.tar .

CMD ["./opcua/bin/opcua",  "-envport", "PORT"]