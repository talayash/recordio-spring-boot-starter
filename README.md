<p align="center">
  <img src="logo.svg" alt="RecordIO Logo" width="150" height="150"/>
</p>

<h1 align="center">RecordIO Spring Boot Starter</h1>

<p align="center">
  <strong>Effortless HTTP request/response recording for Spring Boot applications</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.2+-green.svg" alt="Spring Boot 3.2+"/>
  <img src="https://img.shields.io/badge/Java-17+-blue.svg" alt="Java 17+"/>
  <img src="https://img.shields.io/badge/License-MIT-yellow.svg" alt="License"/>
</p>

---

## Features

- **Annotation-driven** - Simply add `@RecordIO` to controllers or methods
- **Organized folder structure** - Files organized by Controller/Method/request|response
- **Separate request/response files** - Request and response saved independently
- **Automatic masking** - Protect sensitive data like passwords and tokens
- **Flexible output** - JSON or XML format with optional pretty printing
- **Async writing** - Non-blocking file operations for minimal latency impact
- **File rotation** - Automatic cleanup based on file count or age
- **Conditional recording** - Record always, on errors only, or for slow responses

## Quick Start

### 1. Add Dependency

**Maven:**
```xml
<dependency>
    <groupId>io.github.talayash</groupId>
    <artifactId>recordio-spring-boot-starter</artifactId>
    <version>1.0.3</version>
</dependency>
```

**Gradle:**
```groovy
implementation 'io.github.talayash:recordio-spring-boot-starter:1.0.3'
```

### 2. Annotate Your Controller

```java
@RestController
@RecordIO
public class BookingController {

    @PostMapping("/bookings")
    public Booking createBooking(@RequestBody BookingRequest request) {
        // All requests/responses are automatically recorded
        return bookingService.create(request);
    }
}
```

### 3. Check Output

Files are organized in a structured folder hierarchy:

```
records/
└── BookingController/
    └── createBooking/
        ├── request/
        │   └── 20260125_143052_123_a1b2c3d4.json
        └── response/
            └── 20260125_143052_123_a1b2c3d4.json
```

### 4. Configure (Optional)

```yaml
recordio:
  enabled: true
  base-folder: records
  format: JSON
  masking:
    patterns:
      - password
      - token
      - cardNumber
  file-rotation:
    max-files: 100
    max-age-days: 7
```

## Annotation Options

| Option | Default | Description |
|--------|---------|-------------|
| `enabled` | `true` | Enable/disable recording |
| `folder` | `records` | Base output directory |
| `format` | `JSON` | Output format (JSON/XML) |
| `maskFields` | `[]` | Fields to mask |
| `includeHeaders` | `true` | Include HTTP headers |
| `condition` | `ALWAYS` | When to record (ALWAYS/ON_ERROR/SLOW_RESPONSE) |
| `async` | `true` | Write files asynchronously |

## License

MIT
