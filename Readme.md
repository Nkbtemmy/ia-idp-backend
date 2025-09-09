# EduBudget SSO - Identity Provider System

A comprehensive Spring Boot-based Identity Provider (IdP) with JWT authentication, OAuth2 integration, and role-based authorization. Includes a complete high school budget management demo application.

## 🚀 Features

### Core IdP Functionality
- **JWT Authentication**: RSA256-signed access tokens (15min) and refresh tokens (7 days)
- **Email + Password Authentication**: With mandatory email verification
- **LinkedIn OAuth2 Integration**: Social login support
- **JWKS Endpoint**: Public key distribution for token verification
- **Client Registration**: OAuth2 client authentication with Client ID/Secret
- **Role-Based Authorization**: Admin and User roles with hierarchical permissions

## Requirements

- Java 17+
- Maven 3.6+
- Spring Boot

## Setup

### 1. Clone the Repository

```sh
git clone git@github.com:Nkbtemmy/ia-idp-backend.git
cd ia-idp-backend
```

### 2. Generate RSA Keys

You can generate RSA keys using the following command:

```sh
openssl genpkey -algorithm RSA -out private_key.pem -aes256 -pass pass:your_password -pkeyopt rsa_keygen_bits:2048
``` 
Then extract the public key:

```sh
openssl rsa -in private_key.pem -pubout -out public_key.pem -passin pass:your_password
```
You can also use the following commands to generate the keys and convert them to `.key` files:

```sh
openssl genrsa -out private.key 2048
openssl rsa -in private.key -pubout -out public.key
```

Place the generated `private.key` and `public.key` files in the `src/main/resources` directory.
### 3. Configure Application Properties
Edit the `src/main/resources/application.properties` file to set your JWT secret and other configurations:

```properties
private.key.path=private_key.pem
public.key.path=public_key.pem
private.key.password=your_password
jwt.issuer=your_issuer
```
### 4. Build the Project

```sh
mvn clean install
```
### 5. Run the Application

```sh
mvn spring-boot:run
```
### 6. Access the API
You can access the API at `http://localhost:8080/api/v1/sso`.
### 7. Test the Endpoints
You can use tools like Postman or cURL to test the endpoints. Here are some example requests:
### Register User
```http
POST /api/v1/sso/register
Content-Type: application/json

{
  "username": "testuser",
  "password": "testpassword"
}
```
### Login User
```http
POST /api/v1/sso/login
Content-Type: application/json

{
  "username": "testuser",
  "password": "testpassword"
}
```
# ia-idp-backend
