FROM openjdk:8-jre-alpine
COPY build/libs/mi-data-extractor.jar mi-data-extractor.jar
ENTRYPOINT ["java","-jar","/mi-data-extractor.jar"]