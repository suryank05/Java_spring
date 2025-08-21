# 🎓 ExamWizards Backend
## 🌟 Overview

ExamWizards Backend is a robust Spring Boot application that powers a comprehensive online examination and course management system. It provides secure APIs for user management, course creation, exam administration, payment processing, and analytics.

### 🎯 Key Capabilities

- **Multi-Role System**: Support for Students, Instructors, and Administrators
- **Course Management**: Create, manage, and monetize educational content
- **Exam System**: Comprehensive exam creation and grading system
- **Payment Integration**: Secure payment processing with Razorpay
- **Analytics Dashboard**: Detailed insights and reporting
- **Email Notifications**: Automated email system for all user interactions

## ✨ Features

### 🔐 Authentication & Security
- JWT-based authentication
- Role-based access control (RBAC)
- Password encryption with BCrypt
- Email verification system
- Password reset functionality

### 👥 User Management
- Multi-role user system (Student, Instructor, Admin)
- User profile management
- Account verification and activation
- User analytics and reporting

### 📚 Course Management
- Create public/private courses
- Free and paid course options
- Course enrollment system
- Student progress tracking
- Course analytics

### 📝 Exam System
- Multiple question types (MCQ, Descriptive)
- Timed examinations
- Automatic grading system
- Result analytics and leaderboards
- Excel import/export for questions

### 💳 Payment System
- Razorpay payment gateway integration
- Secure payment verification
- Automatic enrollment after payment
- Payment receipt generation

### 📧 Email System
- Gmail SMTP integration
- Automated notifications
- HTML email templates
- Payment receipts and confirmations

### ⭐ Review System
- User reviews and ratings
- Admin moderation system
- Public testimonials display
- Review analytics

### 📊 Analytics & Reporting
- Comprehensive dashboard analytics
- User engagement metrics
- Course performance statistics
- Revenue tracking

## 🛠️ Technology Stack

| Category | Technology |
|----------|------------|
| **Framework** | Spring Boot 3.x |
| **Language** | Java 17+ |
| **Database** | MySQL 8.0+ |
| **ORM** | JPA/Hibernate |
| **Security** | Spring Security + JWT |
| **Payment** | Razorpay Java SDK |
| **Email** | Spring Mail (Gmail SMTP) |
| **Build Tool** | Maven 3.8+ |
| **Documentation** | Swagger/OpenAPI |

## 🚀 Quick Start

### Prerequisites

- ☕ Java 17 or higher
- 🗄️ MySQL 8.0 or higher
- 📦 Maven 3.8 or higher
- 📧 Gmail account for SMTP (optional)
- 💳 Razorpay account for payments (optional)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/examwizards-backend.git
   cd examwizards-backend
   ```

2. **Set up the database**
   ```sql
   CREATE DATABASE examwizards;
   CREATE USER 'examwizards_user'@'localhost' IDENTIFIED BY 'your_password';
   GRANT ALL PRIVILEGES ON examwizards.* TO 'examwizards_user'@'localhost';
   FLUSH PRIVILEGES;
   ```

3. **Configure environment variables**
   ```bash
   cp .env.example .env
   # Edit .env with your actual configuration
   ```

4. **Install dependencies**
   ```bash
   mvn clean install
   ```

5. **Create admin user (Required)**
   ```bash
   # Run the admin creation script
   mysql -u root -p examwizards < create_admin_user.sql
   ```
   
   **Default Admin Credentials:**
   - Username: `admin`
   - Email: `admin@examwizards.com`
   - Password: `admin123`
   
   ⚠️ **Change the default password immediately after first login!**

6. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

The application will start on `http://localhost:8080`

### 🔧 Environment Configuration

Create a `.env` file in the root directory:

```env
# Database Configuration
DB_URL=jdbc:mysql://localhost:3306/examwizards
DB_USERNAME=examwizards_user
DB_PASSWORD=your_db_password

# Email Configuration
EMAIL_USERNAME=your_email@gmail.com
EMAIL_PASSWORD=your_app_password
EMAIL_FROM=ExamWizards <noreply@examwizards.com>

# Razorpay Configuration
RAZORPAY_KEY_ID=your_razorpay_key_id
RAZORPAY_KEY_SECRET=your_razorpay_secret

# Google Gemini AI (for chatbot)
GENAI_API_KEY=your_gemini_api_key

# JWT Configuration
JWT_SECRET=your_jwt_secret_key
```

## 📚 API Documentation

### Base URL
```
http://localhost:8080/api
```

### Authentication Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/auth/login` | User login |
| POST | `/auth/register` | User registration |
| POST | `/auth/request-password-reset` | Password reset request |

### Course Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/courses/public` | Get public courses |
| POST | `/courses/create` | Create new course |
| GET | `/courses/{id}` | Get course details |
| PUT | `/courses/{id}` | Update course |
| DELETE | `/courses/{id}` | Delete course |

### Exam Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/exams` | Get all exams |
| POST | `/exams` | Create new exam |
| GET | `/exams/{id}` | Get exam details |
| POST | `/exams/{id}/submit` | Submit exam answers |

### Payment Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/payment/config` | Get payment configuration |
| POST | `/courses/{id}/purchase` | Initiate course purchase |
| POST | `/courses/payment/verify` | Verify payment |

### Admin Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/admin/dashboard` | Admin dashboard data |
| GET | `/admin/users` | Get all users |
| DELETE | `/admin/users/{id}` | Delete user |

For complete API documentation, visit `/swagger-ui.html` when the application is running.

## 🏗️ Architecture

### Project Structure
```
src/main/java/com/ExamPort/ExamPort/
├── Controller/          # REST API endpoints
├── Service/            # Business logic layer
├── Repository/         # Data access layer
├── Entity/            # JPA entities
├── Security/          # Security configuration
├── Exception/         # Exception handling
├── Config/           # Configuration classes
└── Constants/        # Application constants
```

### Design Patterns
- **MVC Pattern**: Clear separation of concerns
- **Repository Pattern**: Data access abstraction
- **Service Layer Pattern**: Business logic encapsulation
- **DTO Pattern**: Data transfer objects for API responses

### Key Components

#### 🔐 Security Layer
- **JwtUtil**: JWT token generation and validation
- **JwtAuthenticationFilter**: Request authentication
- **SecurityConfig**: Security configuration

#### 🗄️ Data Layer
- **Entities**: JPA entities for database mapping
- **Repositories**: Data access interfaces
- **Services**: Business logic implementation

#### 🌐 API Layer
- **Controllers**: REST API endpoints
- **Exception Handlers**: Global error handling
- **Request/Response DTOs**: Data transfer objects

## 🗃️ Database Schema

### Core Entities

#### Users
```sql
- id (Primary Key)
- username (Unique)
- email (Unique)
- password (Encrypted)
- role (STUDENT, INSTRUCTOR, ADMIN)
- created_at, updated_at
```

#### Courses
```sql
- id (Primary Key)
- name, description
- instructor_id (Foreign Key)
- visibility (PUBLIC, PRIVATE)
- pricing (FREE, PAID)
- price, created_at, updated_at
```

#### Exams
```sql
- id (Primary Key)
- title, description
- course_id (Foreign Key)
- duration, total_marks
- start_date, end_date
- is_active
```

#### Enrollments
```sql
- id (Primary Key)
- student_id (Foreign Key)
- course_id (Foreign Key)
- status (ENROLLED, PAYMENT_PENDING)
- enrollment_date
```

### Entity Relationships
- User → Courses (One-to-Many as Instructor)
- User → Enrollments (One-to-Many as Student)
- Course → Exams (One-to-Many)
- Exam → Questions (One-to-Many)
- User → Results (One-to-Many)

## 🔒 Security

### Authentication Flow
1. User provides credentials
2. Server validates and generates JWT
3. Client stores JWT and includes in requests
4. Server validates JWT on each request

### Security Features
- Password encryption with BCrypt
- JWT token-based authentication
- Role-based access control (STUDENT, INSTRUCTOR, ADMIN)
- CORS configuration for frontend
- Input validation and sanitization
- SQL injection prevention

### Admin Account Security
⚠️ **IMPORTANT**: Admin accounts cannot be created through public registration for security reasons.

- Admin users must be created directly through the database
- Use the provided `create_admin_user.sql` script
- Change default passwords immediately after creation
- See `ADMIN_USER_CREATION_GUIDE.md` for detailed instructions

### API Security
- Protected endpoints require authentication
- Role-based endpoint access
- Admin registration blocked at API level
- Request rate limiting
- Secure headers configuration

## 🧪 Testing

### Running Tests
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=UserServiceTest

# Run tests with coverage
mvn test jacoco:report
```

### Test Categories
- **Unit Tests**: Service layer testing
- **Integration Tests**: API endpoint testing
- **Repository Tests**: Database interaction testing
- **Security Tests**: Authentication and authorization testing


## 📊 Monitoring & Logging

### Application Monitoring
- Spring Boot Actuator endpoints
- Health checks and metrics
- Custom application metrics

### Logging Configuration
- Structured logging with Logback
- Different log levels for different environments
- File-based logging with rotation
- Request/response logging


### Development Guidelines
- Follow Java coding conventions
- Write comprehensive tests
- Update documentation
- Use meaningful commit messages.

## 🙏 Acknowledgments

- Spring Boot team for the excellent framework
- Razorpay for payment gateway integration
- MySQL team for the robust database system
- All contributors who helped build this project
