# File Storage API

[![CI](https://github.com/rahulmaity0/filestorageJava/actions/workflows/ci.yml/badge.svg)](https://github.com/rahulmaity0/filestorageJava/actions/workflows/ci.yml)

A Spring Boot REST API for uploading, listing, downloading and deleting files, with per-user JWT authentication. Image uploads get an automatic thumbnail.

## Features

- Register and log in with username and password (BCrypt), receiving a JWT
- Stateless authentication: every `/files` request needs `Authorization: Bearer <token>`
- Upload files up to 10 MB; each is stored under a unique generated name with its metadata in the database
- Thumbnails generated for image uploads with Thumbnailator
- Users can only see, download and delete their own files

## Tech stack

Java 17, Spring Boot 3.2, Spring Security, Spring Data JPA, H2, JJWT, Thumbnailator, Maven

## Endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/auth/register` | – | Create an account |
| POST | `/auth/login` | – | Get a JWT |
| GET | `/auth/me` | JWT | Current user |
| POST | `/files/upload` | JWT | Upload a file (multipart `file`) |
| GET | `/files/` | JWT | List your files |
| GET | `/files/{fileId}` | JWT | File metadata |
| GET | `/files/download/{fileId}` | JWT | Download a file |
| DELETE | `/files/{fileId}` | JWT | Delete a file |

## Running locally

```bash
mvn spring-boot:run
```

The API starts on `http://localhost:8080`. Data is kept in a file-based H2 database under `./data`, and uploads under `./uploads`. The H2 console is at `/h2-console`.

Set `JWT_SECRET` to your own 256-bit key outside local development.
