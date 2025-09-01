# Spring Batch Application

This project is a Spring Batch application designed for asynchronous and distributed processing of CSV files.

## Project Goal

The main goal of this application is to build a robust and scalable batch processing system. The workflow is as follows:

1.  **File Upload**: Users upload a CSV file through a REST API.
2.  **File Storage**: The uploaded file is temporarily stored in the local file system. In a production environment, this would be replaced with a cloud storage solution like Amazon S3.
3.  **Job Queuing**: A new batch job is created, and a message is sent to a queue. For development, a database-backed queue is used. In production, this will be a message queue like Amazon SQS.
4.  **Asynchronous Processing**: A separate process listens to the queue and processes the jobs asynchronously.
5.  **Distributed & Resilient Processing**: The system is designed to be distributed, ensuring that multiple instances can process jobs concurrently without overlap or duplication.

## Features

-   **Spring Boot**: For building the web application and REST APIs.
-   **Spring Batch**: For the core batch processing logic.
-   **Spring Data JPA**: For database interactions.
-   **Local File Storage**: For storing uploaded files during development.
-   **Database Queue**: A simple queue implementation using a database for local development.
-   **Asynchronous Processing**: Jobs are processed in the background without blocking the user.

## Future Enhancements (Production)

-   **Amazon S3**: For scalable and durable file storage.
-   **Amazon SQS**: For a fully-managed message queue to handle job distribution.
-   **Distributed Locking**: To ensure that jobs are not processed by more than one worker at a time.

## How to Run

1.  **Build the project**:
    ```bash
    mvn clean install
    ```
2.  **Run the application**:
    ```bash
    java -jar target/demo-0.0.1-SNAPSHOT.jar
    ```
3.  **Upload a file**:
    Use a tool like `curl` or Postman to send a `POST` request with a multipart file to the upload endpoint.

