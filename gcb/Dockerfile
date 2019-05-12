FROM ubuntu
ARG NODE_VERSION=v10.15.0
ARG JAVA_11_VERSION=11.0.3.7.1
ARG JAVA_8_VERSION=8.212.04.2
ARG PROJECT_DIR
COPY ${PROJECT_DIR} /project
ENV PATH=/node-${NODE_VERSION}-linux-x64/bin:${PATH}
ENV PATH=/miniconda/bin:${PATH}
ENV JAVA_11_HOME=/amazon-corretto-${JAVA_11_VERSION}-linux-x64
ENV JAVA_8_HOME=/amazon-corretto-${JAVA_8_VERSION}-linux-x64
RUN \
     apt-get update -qq && apt-get install -qq wget xz-utils \
  \
  && wget https://d3pxv6yz143wms.cloudfront.net/${JAVA_11_VERSION}/amazon-corretto-${JAVA_11_VERSION}-linux-x64.tar.gz -q -O jdk-11.tar.gz \
  && tar xf jdk-11.tar.gz \
  \
  && wget https://d3pxv6yz143wms.cloudfront.net/${JAVA_8_VERSION}/amazon-corretto-${JAVA_8_VERSION}-linux-x64.tar.gz -q -O jdk-8.tar.gz \
  && tar xf jdk-8.tar.gz \
  \
  && wget https://nodejs.org/dist/${NODE_VERSION}/node-${NODE_VERSION}-linux-x64.tar.xz -q -O node.tar.xz \
  && tar -xJf node.tar.xz \
  \
  && wget https://repo.continuum.io/miniconda/Miniconda3-latest-Linux-x86_64.sh -q -O miniconda.sh \
  && bash /miniconda.sh -b -p /miniconda \
  && conda config --set always_yes yes \
  && conda create --quiet --name py3 python=3.6.7 \
  \
  && chmod +x /project/gcb/*.sh
