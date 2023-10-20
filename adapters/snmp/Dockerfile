ARG baseimage=amazoncorretto:19
FROM ${baseimage}

WORKDIR /sfc

ADD build/distributions/snmp.tar.gz .

CMD ["./snmp/bin/snmp",  "-envport", "PORT"]