# SSO-Service

A comprehensive Single Sign-On (SSO) authentication service built with Spring Boot, featuring email/password registration with verification, JWT token authentication using RSA keys, and LinkedIn OAuth integration.

## Features

- **Email/Password Authentication**: User registration and login with secure password hashing
- **Email Verification**: Account verification via email with secure tokens
- **JWT Authentication**: Access and refresh tokens using RSA private/public key cryptography
- **LinkedIn OAuth**: Social login integration with LinkedIn
- **CORS Configuration**: Configurable cross-origin resource sharing
- **Security**: Stateless session management, CSRF protection disabled for APIs
- **Database**: PostgreSQL with JPA/Hibernate
- **Email Service**: SMTP email sending with Thymeleaf templates

## API Endpoints

### Authentication Endpoints
- `POST /api/v1/sso-service/auth/register` - User registration
- `POST /api/v1/sso-service/auth/login` - User login
- `POST /api/v1/sso-service/auth/refresh` - Refresh JWT token
- `POST /api/v1/sso-service/auth/logout` - Logout (revoke refresh token)
- `POST /api/v1/sso-service/auth/logout-all` - Logout from all devices
- `GET /api/v1/sso-service/auth/verify` - Email verification
- `POST /api/v1/sso-service/auth/resend-verification` - Resend verification email
- `POST /api/v1/sso-service/auth/validate-token` - Validate JWT token
- `GET /api/v1/sso-service/auth/check-email` - Check email availability

### LinkedIn OAuth Endpoints
- `GET /api/v1/sso-service/auth/linkedin/login` - Initiate LinkedIn OAuth
- `GET /api/v1/sso-service/auth/linkedin/callback` - LinkedIn OAuth callback
- `GET /api/v1/sso-service/auth/linkedin/status` - OAuth status

## Setup Instructions

### Prerequisites
- Java 17 or higher
- Maven 3.6+
- PostgreSQL database
- SMTP email server access
- LinkedIn Developer App (for OAuth)

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
