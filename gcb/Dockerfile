FROM openjdk:8u181
COPY . /project
ARG NODE_VERSION=v10.13.0
ENV PATH=/node-${NODE_VERSION}-linux-x64/bin:${PATH}
ENV PATH=/miniconda/bin:${PATH}
RUN \
     wget https://nodejs.org/dist/${NODE_VERSION}/node-${NODE_VERSION}-linux-x64.tar.xz -O node.tar.xz \
  && tar -xJvf node.tar.xz \
  \
  && wget https://repo.continuum.io/miniconda/Miniconda3-latest-Linux-x86_64.sh -O miniconda.sh \
  && bash /miniconda.sh -b -p /miniconda \
  && conda config --set always_yes yes \
  && conda create --quiet --name py3 python=3.7.1 \
  && conda create --quiet --name py2 python=2.7.15 \
  \
  && chmod +x /project/gcb/start.sh
CMD ["/project/gcb/start.sh"]
