# 🔖 Logging Quick Reference - Standard for All Projects

## ✅ Use This Pattern Always

### 1. Add Lombok @Slf4j
```java
@Slf4j
@Service  // or @Controller, @Component, etc.
public class MyService {
    // 'log' is automatically available
}
```

### 2. Log Levels
```java
log.error("Error occurred", exception);  // Something failed
log.warn("Warning message");             // Potential issue
log.info("Business event");              // Important info
log.debug("Debug details");              // Development
```

### 3. Parameterized Logging (ALWAYS)
```java
// ✅ CORRECT
log.info("User {} logged in from {}", username, ipAddress);

// ❌ NEVER DO THIS
log.info("User " + username + " logged in from " + ipAddress);
```

### 4. Include Exceptions
```java
try {
    // risky code
} catch (Exception e) {
    log.error("Failed to process: {}", item, e);  // 'e' at the end!
}
```

---

## 📋 Common Patterns

### Success Messages
```java
log.info("✓ Email sent successfully to: {}", email);
log.info("✓ Document uploaded - ID: {}, Type: {}", id, type);
```

### Error Messages
```java
log.error("✗ Failed to send email to: {}", email, exception);
log.error("✗ Could not process document: {}", docId, exception);
```

### Warning Messages
```java
log.warn("Duplicate detected - Type: {}, Number: {}", type, number);
log.warn("Slow query detected - Duration: {}ms", duration);
```

### Scheduled Tasks
```java
log.info("Running scheduled task at: {}", LocalDateTime.now());
log.info("Completed scheduled task - Processed: {} items", count);
```

---

## ❌ Never Do This

```java
// ❌ Don't use System.out
System.out.println("Something");

// ❌ Don't use System.err
System.err.println("Error");

// ❌ Don't concatenate strings
log.info("User " + name + " did " + action);

// ❌ Don't log sensitive data
log.info("Password: {}", password);

// ❌ Don't lose exception details
log.error("Error: " + e.getMessage());
```

---

## ✅ Always Do This

```java
// ✅ Use @Slf4j
@Slf4j
@Service
public class MyService { }

// ✅ Use parameterized logging
log.info("Action: {}, User: {}", action, user);

// ✅ Include exceptions
log.error("Failed: {}", operation, exception);

// ✅ Use appropriate levels
log.error() - Errors
log.warn()  - Warnings
log.info()  - Important events
log.debug() - Debug info
```

---

## 🎯 Remember for ALL Projects

1. **Always add `@Slf4j`** to classes that need logging
2. **Always use placeholders `{}`** instead of concatenation
3. **Always include exceptions** as last parameter in log.error()
4. **Never log** passwords, tokens, or sensitive data
5. **Use appropriate levels** - info for business events, error for failures

---

**This standard applies to all Java projects in this workspace!**

