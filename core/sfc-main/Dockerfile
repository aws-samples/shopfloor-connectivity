ARG baseimage=amazoncorretto:19
FROM ${baseimage}

WORKDIR /sfc

ADD ./build/distributions/sfc-main.tar .

# config is read from config dir which is a mounted from a host directory that contains the actual config file
CMD ["./sfc-main/bin/sfc-main",  "-config",  "./config/config.json", "-info"]