# Quick Start: Excel-Based ROTA Parsing

## What's New? 🎉

Your ROTA parsing system now supports **Excel-like table structures** and **compressed time formats**!

### New Time Format Supported: ✅
- **`08:18:00`** automatically converts to **`08:00-18:00`**
  - First 2 digits (08) = Check-in hour
  - Next 2 digits (18) = Check-out hour

## How to Use

### 1. Enable Excel Parsing (Already Done!)
The system is configured to use Excel-based parsing by default.

Location: `RotaService.java` line 56
```java
private static final boolean USE_EXCEL_PARSING = true; // ✅ Enabled
```

### 2. Upload Your ROTA
Just upload your ROTA image as before:
- Frontend: Use the ROTA upload form
- Backend: POST to `/api/rota/upload`

### 3. Supported Formats

#### Time Formats:
```
✅ 08:18:00     →  08:00-18:00  (compressed format)
✅ 08:00-18:00  →  08:00-18:00  (standard format)
✅ 17:00-03:00  →  17:00-03:00  (night shift)
✅ OFF          →  Off Day
✅ Holiday      →  Off Day
✅ Leave        →  Off Day
```

#### Date Formats:
```
✅ 01/12/2024
✅ 01-12-2024
✅ 1/12/2024
✅ 1-12-2024
```

## Installation Steps

### 1. Install Dependencies
```cmd
cd C:\Users\waseem.uddin\EmployeeManagementSystem
mvn clean install
```

### 2. Verify Dependencies
Check that `pom.xml` contains:
```xml
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi</artifactId>
    <version>5.2.3</version>
</dependency>
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.3</version>
</dependency>
```

### 3. Run the Application
```cmd
mvn spring-boot:run
```

## Expected Log Output

When you upload a ROTA, you should see:
```
📊 Starting Excel-based ROTA parsing for 25 employees
📝 Extracted 5423 characters of structured text
📋 Created table with 28 rows
📅 Found 7 dates in header
👤 Found employee: John Doe in row 5
🕐 Converted compressed time '08:18:00' to '08:00-18:00'
👤 Found employee: Jane Smith in row 6
🕐 09:00-17:00 (standard format)
✅ Excel-based parsing found 175 schedules
```

## Testing the Compressed Time Format

### Example ROTA Cell Values:
```
Employee Name | 01/12/24 | 02/12/24 | 03/12/24
John Doe      | 08:18:00 | 09:17:00 | OFF
Jane Smith    | 08:00-18:00 | 17:00-03:00 | Holiday
```

### Expected Database Records:
```
| Employee   | Date       | Start Time | End Time | Duty         | Is Off Day |
|------------|------------|------------|----------|--------------|------------|
| John Doe   | 01/12/2024 | 08:00      | 18:00    | 08:00-18:00  | false      |
| John Doe   | 02/12/2024 | 09:00      | 17:00    | 09:00-17:00  | false      |
| John Doe   | 03/12/2024 | null       | null     | OFF          | true       |
| Jane Smith | 01/12/2024 | 08:00      | 18:00    | 08:00-18:00  | false      |
| Jane Smith | 02/12/2024 | 17:00      | 03:00    | 17:00-03:00  | false      |
| Jane Smith | 03/12/2024 | null       | null     | Holiday      | true       |
```

## Switching Between Parsing Methods

### Use Excel-Based Parsing (Recommended)
```java
private static final boolean USE_EXCEL_PARSING = true;
```
- ✅ Better for table structures
- ✅ Handles compressed times
- ✅ More accurate employee matching

### Use Traditional OCR Parsing
```java
private static final boolean USE_EXCEL_PARSING = false;
```
- ⚠️ Use if Excel parsing has issues
- ⚠️ Doesn't support compressed times
- ⚠️ Less accurate with colored cells

## Troubleshooting

### Problem: No schedules found
**Solutions:**
1. Check employee names in database match ROTA exactly
2. Verify date format is correct (dd/MM/yyyy)
3. Ensure image quality is good (not blurry)

### Problem: Compressed times not converting
**Check:**
1. Format is exactly `HH:MM:SS` (e.g., `08:18:00`)
2. No extra spaces or characters
3. Excel parsing is enabled (`USE_EXCEL_PARSING = true`)

### Problem: Wrong employees matched
**Fix:**
1. Update employee full names in database
2. Check for typos in ROTA image
3. Review logs for fuzzy matching details

## Debug Mode

Enable detailed logging in `application.properties`:
```properties
logging.level.com.was.employeemanagementsystem.service=DEBUG
```

Then check logs for:
- 🔍 Table structure detected
- 👤 Employee matching details
- 🕐 Time conversion details
- ✅ Schedule creation success

## Files Modified

1. ✅ `pom.xml` - Added Apache POI dependencies
2. ✅ `RotaService.java` - Integrated Excel parser
3. ✅ `ExcelRotaParser.java` - New parser implementation

## Quick Commands

```cmd
# Install dependencies
mvn clean install

# Run application
mvn spring-boot:run

# View logs
tail -f logs/application.log

# Test ROTA upload
curl -X POST http://localhost:8080/api/rota/upload \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "file=@rota.png"
```

## What's Next?

1. ✅ Test with your actual ROTA images
2. ✅ Verify compressed times convert correctly
3. ✅ Check employee matching accuracy
4. ✅ Review parsed schedules in database
5. ✅ Provide feedback for improvements

---

**Ready to use!** Just run `mvn clean install` and upload your ROTA! 🚀

For detailed documentation, see: `EXCEL_BASED_ROTA_PARSING.md`

