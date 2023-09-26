# template for default docker file
ARG os
ARG target

FROM ${baseimage}

WORKDIR /sfc

ADD ./build/distributions/mqtt.tar .

CMD ["./${target}/bin/${target}",  "-envport", "PORT"]