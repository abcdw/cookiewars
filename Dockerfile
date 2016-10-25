FROM java:8-alpine
MAINTAINER Your Name <you@example.com>

ADD target/uberjar/cookiewars.jar /cookiewars/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/cookiewars/app.jar"]
