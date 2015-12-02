FROM java:8
COPY . /usr/yass
WORKDIR /usr/yass
EXPOSE 9090
CMD ["java", "-classpath", "libs/*", "ch.softappeal.yass.tutorial.acceptor.web.JettyAcceptor"]
