ARG baseimage=amazoncorretto:19
FROM ${baseimage}

WORKDIR /sfc

ADD build/distributions/modbus-tcp.tar.gz .

CMD ["./modbus-tcp/bin/modbus-tcp",  "-envport", "PORT"]