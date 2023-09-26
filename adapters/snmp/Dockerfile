ARG baseimage=amazoncorretto:19
FROM ${baseimage}

WORKDIR /sfc

ADD build/distributions/snmp.tar .

CMD ["./snmp/bin/snmp",  "-envport", "PORT"]