FROM ubuntu:18.04
ARG JAVA_VERSION=11.0.4.11.1
ARG NODE_VERSION=v10.15.0
ARG PYTHON_VERSION=3.6
COPY . /yass
ENV PATH=/amazon-corretto-${JAVA_VERSION}-linux-x64/bin:/node-${NODE_VERSION}-linux-x64/bin:${PATH}
WORKDIR /yass
RUN \
  apt-get update -qq && apt-get install -qq wget xz-utils python${PYTHON_VERSION} python3-pip && \
  \
  wget https://d3pxv6yz143wms.cloudfront.net/${JAVA_VERSION}/amazon-corretto-${JAVA_VERSION}-linux-x64.tar.gz -q -O /jdk.tar.gz && \
  tar xf /jdk.tar.gz -C .. && \
  \
  wget https://nodejs.org/dist/${NODE_VERSION}/node-${NODE_VERSION}-linux-x64.tar.xz -q -O /node.tar.xz && \
  tar -xJf /node.tar.xz -C .. && \
  \
  ln -s /usr/bin/python${PYTHON_VERSION} /usr/bin/python && \
  \
  chmod +x *.sh
