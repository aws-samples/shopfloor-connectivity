ARG baseimage=amazoncorretto:19
FROM ${baseimage}

WORKDIR /sfc

ADD build/distributions/ads.tar.gz .

CMD ["./pccc/bin/ads",  "-envport", "PORT"]