# Investment Portfolio Tracker (Backend) 📈

A comprehensive investment portfolio tracking system built with enterprise-grade Spring architecture. This RESTful API enables users to manage their investment portfolios, track stock performance, and monitor their financial growth over time.

## 🚀 Features

- **User Management**: Secure user registration and authentication with JWT tokens
- **Portfolio Management**: Create and manage multiple investment portfolios
- **Stock Integration**: Real-time stock data integration via Alpha Vantage API
- **Investment Tracking**: Add, update, and remove investments from portfolios
- **Performance Analytics**: Track portfolio performance and individual stock metrics
- **Secure Authentication**: JWT-based authentication with Spring Security
- **Data Persistence**: Robust PostgreSQL database with Flyway migrations

## 🛠️ Tech Stack

- **Java 17** - Modern Java features for clean, efficient code
- **Spring Boot 3.4.1** - Enterprise-grade application framework
- **Spring Security** - Comprehensive security framework
- **Spring Data JPA** - Simplified data access layer
- **PostgreSQL** - Reliable relational database
- **Flyway** - Database version control and migration
- **Alpha Vantage API** - Real-time stock market data
- **JWT (JJWT)** - Secure token-based authentication
- **Maven** - Dependency management and build automation
- **JUnit 5** - Modern testing framework
- **Testcontainers** - Integration testing with real database instances
- **Lombok** - Reduced boilerplate code

## 📋 Prerequisites

Before running this application, make sure you have:

- Java 17 or higher
- Maven 3.6+
- PostgreSQL 12+
- Alpha Vantage API key (free tier available)

## ⚙️ Setup & Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/mattbixby123/portfolio-tracker-backend.git
   cd portfolio-tracker-backend
   ```

2. **Set up PostgreSQL database**
   ```sql
   CREATE DATABASE portfolio_tracker;
   CREATE USER portfolio_user WITH PASSWORD 'your_password';
   GRANT ALL PRIVILEGES ON DATABASE portfolio_tracker TO portfolio_user;
   ```

3. **Configure environment variables**
   Create an `application-local.properties` file:
   ```properties
   # Database Configuration
   spring.datasource.url=jdbc:postgresql://localhost:5432/portfolio_tracker
   spring.datasource.username=portfolio_user
   spring.datasource.password=your_password
   
   # Alpha Vantage API
   alphavantage.api.key=YOUR_ALPHA_VANTAGE_API_KEY
   alphavantage.api.base-url=https://www.alphavantage.co/query
   
   # JWT Configuration
   jwt.secret=your-secret-key-here
   jwt.expiration=86400000
   ```

4. **Get Alpha Vantage API Key**
    - Visit [Alpha Vantage](https://www.alphavantage.co/support/#api-key)
    - Sign up for a free API key (500 requests/day, 5 requests/minute)
    - Add the key to your configuration

5. **Run the application**
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=local
   ```

The application will start on `http://localhost:8080`

## 🔗 API Endpoints

### Authentication (`/api/v1/auth`)
- `POST /register` - Register new user
- `POST /login` - User login
- `POST /toggle-role` - Toggle user role (admin only)

### User Management (`/api/v1/users`)
- `GET /profile` - Get current user profile
- `PUT /profile` - Update user profile
- `POST /change-password` - Change user password
- `GET /admin/all` - Get all users (admin only)
- `PUT /admin/{id}` - Update user by ID (admin only)
- `POST /admin/{id}/toggle-enabled` - Toggle user enabled status (admin only)
- `DELETE /admin/{id}` - Delete user (admin only)

### Portfolio Management (`/api/v1/portfolio`)
- `GET /summary` - Get portfolio summary with metrics
- `GET /performance` - Get portfolio performance metrics
- `GET /top-holdings` - Get top holdings (default: 5)
- `GET /monthly-summary` - Get monthly transaction summary
- `GET /allocation` - Get sector allocation

### Positions (`/api/v1/positions`)
- `GET /` - Get all user positions
- `GET /{id}` - Get position by ID
- `GET /value` - Get total portfolio value
- `GET /largest` - Get largest positions (default: 5)
- `GET /gains` - Get positions with gains above threshold
- `GET /sector-allocation` - Get sector allocation breakdown

### Stock Management (`/api/v1/stocks`)
- `GET /` - Get all stocks
- `GET /{id}` - Get stock by ID
- `GET /ticker/{ticker}` - Get stock by ticker symbol
- `GET /search` - Search stocks by query
- `GET /top` - Get top stocks by price
- `GET /sectors/average-price` - Get average price by sector
- `POST /` - Create new stock
- `PUT /{id}` - Update stock details
- `PATCH /{id}/price` - Update stock price
- `PATCH /ticker/{ticker}/price` - Update stock price by ticker
- `DELETE /{id}` - Delete stock
- `POST /cache/clear` - Clear stock price cache
- `PUT /ticker/{ticker}/refresh` - Refresh stock price from Alpha Vantage
- `PUT /refresh-all` - Refresh all stock prices
- `POST /lookup/{ticker}` - Add new stock from Alpha Vantage

### Transactions (`/api/v1/transactions`)
- `GET /` - Get all user transactions
- `GET /paged` - Get paginated transactions
- `GET /stock/{stockId}` - Get transactions for specific stock
- `GET /date-range` - Get transactions within date range
- `GET /monthly-summary` - Get monthly transaction summary
- `POST /buy` - Execute buy transaction
- `POST /sell` - Execute sell transaction

### Example Request/Response

**POST /api/v1/auth/register**
```json
{
  "email": "investor@example.com",
  "password": "SecurePass123!",
  "firstName": "John",
  "lastName": "Doe"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "email": "investor@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "tokenType": "Bearer",
  "role": "USER"
}
```

**POST /api/v1/transactions/buy**
```json
{
   "stockTicker": "AAPL",
   "quantity": 10,
   "price": 150.25,
   "fee": 0.00,
   "transactionType": "BUY"
}
```
fee (optional): Transaction cost (e.g. brokerage fee). Defaults to 0.00 if not provided.

## 🧪 Testing

The application includes comprehensive test coverage for controller and service layers.

**Run all tests:**
```bash
mvn test


**Run specific test classes:**
```bash
mvn test -Dtest=PortfolioServiceTest
mvn test -Dtest=AuthControllerTest
```

**Test Coverage Includes:**
- Unit tests for service layer business logic
- Integration tests with Testcontainers
- Controller tests with MockMvc
- Repository tests with @DataJpaTest

## 🏗️ Database Schema

The application uses Flyway for database migrations. Key entities include:

- **Users** - User account information with JWT authentication
- **Stocks** - Stock metadata, pricing, and sector information
- **Positions** - User's current stock holdings with calculated metrics
- **Transactions** - Buy/sell transaction history with fees and dates
- **Performance tracking** - Calculated fields for gains, losses, and portfolio metrics

## 📊 Alpha Vantage Integration

This application integrates with Alpha Vantage's free tier API:

- **Rate Limits**: 500 requests/day, 5 requests/minute
- **Cached Responses**: Stock prices cached for 15 minutes to optimize API usage
- **Error Handling**: Graceful fallbacks when API limits are reached
- **Supported Functions**: Real-time quotes, historical data, company overviews

## 🔒 Security Features

- JWT-based stateless authentication
- Password encryption with BCrypt
- CORS configuration for frontend integration
- Input validation and sanitization
- SQL injection prevention with JPA

## 🚀 Future Enhancements

- [ ] Frontend React application (in development)
- [ ] Real-time portfolio performance dashboards
- [ ] Email notifications for significant price changes
- [ ] Advanced portfolio analytics and reporting
- [ ] Support for additional asset types (bonds, crypto)
- [ ] Mobile app integration
- [ ] Social features (portfolio sharing, leaderboards)

## 🤝 Contributing

This is a personal portfolio project, but feedback and suggestions are welcome! Feel free to:

1. Open an issue for bugs or feature requests
2. Fork the repository for your own experiments
3. Reach out with questions or suggestions

## 📧 Contact

**Matthew Bixby**
- GitHub: [@mattbixby123](https://github.com/mattbixby123)
- LinkedIn: [Matthew Bixby](https://linkedin.com/in/matthew-bixby)
- Email: matthew.bixby123@gmail.com

## 📝 License

This project is open source and available under the [MIT License](LICENSE).

---

*Built with ❤️ using Spring Boot and modern Java development practices.*