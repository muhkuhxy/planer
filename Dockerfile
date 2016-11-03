FROM 1science/sbt:0.13.8-oracle-jre-8
MAINTAINER Timon Loeffen <timon.loeffen@gmail.com>
RUN apk-install \
   nodejs
WORKDIR /vagrant
EXPOSE 9000
ENTRYPOINT ["bash"]

