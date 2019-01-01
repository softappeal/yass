FROM openjdk:8u181
ARG NODE_VERSION=v10.15.0
ARG PROJECT_DIR
COPY ${PROJECT_DIR} /project
ENV PATH=/node-${NODE_VERSION}-linux-x64/bin:${PATH}
ENV PATH=/miniconda/bin:${PATH}
ENV JAVA_11_HOME=/jdk-11.0.1
RUN \
     wget https://download.java.net/java/GA/jdk11/13/GPL/openjdk-11.0.1_linux-x64_bin.tar.gz -q -O openjdk-11.tar.gz \
  && tar xf openjdk-11.tar.gz \
  \
  && wget https://nodejs.org/dist/${NODE_VERSION}/node-${NODE_VERSION}-linux-x64.tar.xz -q -O node.tar.xz \
  && tar -xJf node.tar.xz \
  \
  && wget https://repo.continuum.io/miniconda/Miniconda3-latest-Linux-x86_64.sh -q -O miniconda.sh \
  && bash /miniconda.sh -b -p /miniconda \
  && conda config --set always_yes yes \
  && conda create --quiet --name py3 python=3.7.1 \
  && conda create --quiet --name py2 python=2.7.15 \
  \
  && chmod +x /project/gcb/*.sh
