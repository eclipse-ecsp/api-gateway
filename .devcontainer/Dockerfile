FROM eclipse-temurin:17.0.15_6-jdk-jammy
LABEL authors="akumar87"

RUN apt-get -y update \
    && apt update \
    && apt -y install maven \
    && apt-get -y install curl \
    && apt-get -y install git

CMD ["/bin/bash"]