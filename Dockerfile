FROM ubuntu:latest

# Use Tomcat 10.1.x
ENV TOMCAT_VERSION 10.1.20
ENV CATALINA_HOME /opt/tomcat
ENV JAVA_HOME /usr/lib/jvm/java-21-openjdk-amd64
ENV PATH $CATALINA_HOME/bin:$PATH

# Install JDK & wget packages
RUN apt-get update && \
    apt-get install -y openjdk-21-jdk wget && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Install Tomcat 10
RUN mkdir -p $CATALINA_HOME && \
    wget -q https://archive.apache.org/dist/tomcat/tomcat-10/v${TOMCAT_VERSION}/bin/apache-tomcat-${TOMCAT_VERSION}.tar.gz -O /tmp/tomcat.tar.gz && \
    tar xzf /tmp/tomcat.tar.gz -C /tmp && \
    cp -rv /tmp/apache-tomcat-${TOMCAT_VERSION}/* $CATALINA_HOME && \
    rm -rf /tmp/apache-tomcat-${TOMCAT_VERSION} /tmp/tomcat.tar.gz

# Change Tomcat port to 8085
RUN sed -i 's/port="8080"/port="8085"/g' $CATALINA_HOME/conf/server.xml

# Remove default webapps
RUN rm -rf $CATALINA_HOME/webapps/*

# Make scripts executable
RUN chmod +x $CATALINA_HOME/bin/*.sh

# Copy WAR file
COPY target/mysampleapp.war $CATALINA_HOME/webapps/ROOT.war

EXPOSE 8085

CMD ["catalina.sh", "run"]