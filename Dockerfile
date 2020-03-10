###
# vert.x docker example using a Java verticle
# To build:
#  mvn clean package
#  docker build -t dpd-vertx-service .
# To run:
#   docker run -t -i -p 8080:8888 dpd-vertx-service
#   docker run -d -p 8080:8888 dpd-vertx-service
###

# Extend vert.x image
FROM adoptopenjdk/openjdk13:ubi

ENV VERTICLE_FILE dpd-service-1.0.0-SNAPSHOT-fat.jar

# Set the location of the verticles
ENV VERTICLE_HOME /usr/verticles

EXPOSE 8888

# Copy your verticle to the container
COPY ./target/$VERTICLE_FILE $VERTICLE_HOME/

# Launch the verticle
WORKDIR $VERTICLE_HOME
ENTRYPOINT ["sh", "-c"]
CMD ["exec java -jar $VERTICLE_FILE"]
