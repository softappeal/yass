FROM ubuntu
ARG NODE_VERSION=v10.15.0
ARG PROJECT_DIR
COPY ${PROJECT_DIR} /project
ENV PATH=/node-${NODE_VERSION}-linux-x64/bin:${PATH}
ENV PATH=/miniconda/bin:${PATH}
ENV JAVA_11_HOME=/jdk-11.0.2
ENV JAVA_8_HOME=/jdk1.8.0_201
RUN \
     apt-get update -qq && apt-get install -qq wget xz-utils \
  \
  && wget https://download.java.net/java/GA/jdk11/9/GPL/openjdk-11.0.2_linux-x64_bin.tar.gz -q -O openjdk-11.tar.gz \
  && tar xf openjdk-11.tar.gz \
  \
  && wget --no-cookies --no-check-certificate --header "Cookie: gpw_e24=https%3A%2F%2Fwww.oracle.com%2Ftechnetwork%2Fjava%2Fjavase%2Fdownloads%2Fjdk8-downloads-2133151.html; oraclelicense=accept-securebackup-cookie;" "https://download.oracle.com/otn-pub/java/jdk/8u201-b09/42970487e3af4f5aa5bca3f542482c60/jdk-8u201-linux-x64.tar.gz" -q -O jdk-8.tar.gz \
  && tar xf jdk-8.tar.gz \
  \
  && wget https://nodejs.org/dist/${NODE_VERSION}/node-${NODE_VERSION}-linux-x64.tar.xz -q -O node.tar.xz \
  && tar -xJf node.tar.xz \
  \
  && wget https://repo.continuum.io/miniconda/Miniconda3-latest-Linux-x86_64.sh -q -O miniconda.sh \
  && bash /miniconda.sh -b -p /miniconda \
  && conda config --set always_yes yes \
  && conda create --quiet --name py3 python=3.6.7 \
  && conda create --quiet --name py2 python=2.7.15 \
  \
  && chmod +x /project/gcb/*.sh
