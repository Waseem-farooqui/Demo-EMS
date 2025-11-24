# Production Resource Recommendations
## Employee Management System

### Scenario 1: Small Business (5 Concurrent Users, 200 Employees)
**User Base:**
- Total Employees: 200
- Concurrent Users: 5
- Documents per Employee: 5 (max)
- Total Documents: 200 × 5 = **1,000 documents**

**Document Characteristics:**
- Average document size: 2 MB
- Total document storage: 1,000 × 2 MB = **2 GB**
- Peak concurrent users: 5
- Document processing: OCR-intensive but low volume

---

### Scenario 2: Enterprise (5,000 Users, 250,000 Documents)
**User Base:**
- Total Users: 5,000
- Documents per User: 50
- Total Documents: 250,000

**Document Characteristics:**
- Average document size: 2-5 MB (passports, visas, contracts, PDFs)
- Total document storage: 250,000 × 3 MB (average) = **750 GB**
- Peak concurrent users: ~10-20% = 500-1,000 users
- Document processing: OCR-intensive operations

---

## Recommended Infrastructure

### For Small Business (5 Concurrent Users, 200 Employees)

#### Minimum Viable Production:
- **CPU**: 4 cores (8 vCPUs)
  - 2 cores for backend
  - 1 core for MySQL
  - 1 core for system overhead
  - *Rationale: Low concurrent load, minimal processing*

- **RAM**: 8 GB
  - MySQL: 2 GB
  - Backend: 4 GB
  - Frontend: 512 MB
  - System/OS: 1.5 GB
  - *Rationale: Sufficient for 1,000 documents*

- **Storage**: 50 GB SSD
  - Documents: 2 GB
  - Database: 5 GB (with indexes, logs)
  - System/OS: 20 GB
  - Docker images: 5 GB
  - Backups: 10 GB (3-day retention)
  - Growth buffer: 8 GB
  - *Rationale: Small dataset, minimal storage needs*

#### Recommended Production:
- **CPU**: 8 cores (16 vCPUs)
  - 4 cores for backend
  - 2 cores for MySQL
  - 2 cores for system overhead
  - *Rationale: Comfortable headroom for growth*

- **RAM**: 16 GB
  - MySQL: 4 GB
  - Backend: 8 GB
  - Frontend: 1 GB
  - System/OS: 2 GB
  - Buffer: 1 GB
  - *Rationale: Better performance, room for expansion*

- **Storage**: 100 GB SSD
  - Documents: 2 GB
  - Database: 10 GB
  - System/OS: 20 GB
  - Docker images: 10 GB
  - Backups: 30 GB (7-day retention)
  - Growth buffer: 28 GB
  - *Rationale: Comfortable for growth to 500-1,000 employees*

#### Estimated Cost (Cloud):
- AWS EC2: `t3.medium` (2 vCPU, 4 GB RAM) - ~$30/month (minimum)
- AWS EC2: `t3.large` (2 vCPU, 8 GB RAM) - ~$60/month (recommended)
- AWS EC2: `t3.xlarge` (4 vCPU, 16 GB RAM) - ~$120/month (comfortable)
- Storage: EBS gp3 100GB - ~$10/month
- **Total: ~$40-130/month**

#### Docker Resource Limits (compose.yaml):
```yaml
services:
  mysql:
    deploy:
      resources:
        limits:
          cpus: '2.0'
          memory: 4G
        reservations:
          cpus: '1.0'
          memory: 2G

  backend:
    deploy:
      resources:
        limits:
          cpus: '4.0'
          memory: 8G
        reservations:
          cpus: '2.0'
          memory: 4G

  frontend:
    deploy:
      resources:
        limits:
          cpus: '1.0'
          memory: 1G
        reservations:
          cpus: '0.5'
          memory: 512M
```

---

### For Enterprise (5,000 Users, 250,000 Documents)

### Option 1: Standard Production (Recommended)
**For: Stable production with good performance**

#### Server Specifications:
- **CPU**: 16 cores (32 vCPUs)
  - 8 cores for backend (document processing, OCR)
  - 4 cores for MySQL
  - 4 cores for system overhead
  - *Rationale: OCR and image processing are CPU-intensive*

- **RAM**: 32 GB
  - MySQL: 8 GB (buffer pool for 250K documents)
  - Backend: 12 GB (JVM heap + OCR processing)
  - Frontend: 1 GB
  - System/OS: 3 GB
  - Buffer: 8 GB (for peak loads)
  - *Rationale: Large document processing requires significant memory*

- **Storage**: 2 TB SSD (NVMe recommended)
  - Documents: 750 GB
  - Database: 200 GB (with indexes, logs, backups)
  - System/OS: 50 GB
  - Docker images: 20 GB
  - Backups: 500 GB (daily backups, 7-day retention)
  - Growth buffer: 480 GB (20% for future growth)
  - *Rationale: Fast I/O for document uploads/downloads*

#### Estimated Cost (Cloud):
- AWS EC2: `m5.4xlarge` (16 vCPU, 64 GB RAM) - ~$600/month
- Storage: EBS gp3 2TB - ~$200/month
- **Total: ~$800/month**

---

### Option 2: High-Performance Production
**For: High-traffic, low-latency requirements**

#### Server Specifications:
- **CPU**: 32 cores (64 vCPUs)
  - 16 cores for backend
  - 8 cores for MySQL
  - 8 cores for system overhead
  - *Rationale: Handle concurrent document processing*

- **RAM**: 64 GB
  - MySQL: 16 GB
  - Backend: 24 GB
  - Frontend: 2 GB
  - System/OS: 4 GB
  - Buffer: 18 GB
  - *Rationale: Process multiple documents simultaneously*

- **Storage**: 3 TB NVMe SSD
  - Documents: 750 GB
  - Database: 300 GB
  - System/OS: 50 GB
  - Docker images: 30 GB
  - Backups: 1 TB (extended retention)
  - Growth buffer: 870 GB
  - *Rationale: Extended backup retention and faster I/O*

#### Estimated Cost (Cloud):
- AWS EC2: `m5.8xlarge` (32 vCPU, 128 GB RAM) - ~$1,200/month
- Storage: EBS gp3 3TB - ~$300/month
- **Total: ~$1,500/month**

---

### Option 3: Budget-Conscious Production
**For: Cost-optimized with acceptable performance**

#### Server Specifications:
- **CPU**: 8 cores (16 vCPUs)
  - 4 cores for backend
  - 2 cores for MySQL
  - 2 cores for system overhead
  - *Rationale: Minimum for acceptable performance*

- **RAM**: 16 GB
  - MySQL: 4 GB
  - Backend: 8 GB
  - Frontend: 512 MB
  - System/OS: 2 GB
  - Buffer: 1.5 GB
  - *Rationale: May experience slowdowns during peak loads*

- **Storage**: 1.5 TB SSD
  - Documents: 750 GB
  - Database: 150 GB
  - System/OS: 50 GB
  - Docker images: 20 GB
  - Backups: 300 GB (3-day retention)
  - Growth buffer: 230 GB
  - *Rationale: Reduced backup retention to save costs*

#### Estimated Cost (Cloud):
- AWS EC2: `m5.2xlarge` (8 vCPU, 32 GB RAM) - ~$300/month
- Storage: EBS gp3 1.5TB - ~$150/month
- **Total: ~$450/month**

---

## Docker Resource Limits (compose.yaml)

### Recommended Configuration:

```yaml
services:
  mysql:
    deploy:
      resources:
        limits:
          cpus: '4.0'
          memory: 8G
        reservations:
          cpus: '2.0'
          memory: 4G

  backend:
    deploy:
      resources:
        limits:
          cpus: '8.0'
          memory: 12G
        reservations:
          cpus: '4.0'
          memory: 8G

  frontend:
    deploy:
      resources:
        limits:
          cpus: '1.0'
          memory: 1G
        reservations:
          cpus: '0.5'
          memory: 512M
```

---

## Database Optimization

### MySQL Configuration:
```ini
# For 250K documents, optimize buffer pool
innodb_buffer_pool_size = 8G
innodb_log_file_size = 512M
max_connections = 500
query_cache_size = 256M
```

### JPA/Hibernate Settings:
```properties
# Batch processing for bulk operations
spring.jpa.properties.hibernate.jdbc.batch_size=50
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true

# Connection pool
spring.datasource.hikari.maximum-pool-size=50
spring.datasource.hikari.minimum-idle=10
```

---

## Storage Breakdown

### Document Storage:
- **Average per document**: 3 MB
- **Total documents**: 250,000
- **Total storage needed**: 750 GB
- **With redundancy (2x)**: 1.5 TB

### Database Storage:
- **User data**: ~50 MB (5,000 users × 10 KB)
- **Document metadata**: ~500 MB (250,000 × 2 KB)
- **Indexes**: ~200 MB
- **Logs**: ~50 GB (rotating)
- **Total**: ~50-100 GB (with growth buffer: 200 GB)

### Backup Storage:
- **Daily backups**: 800 GB × 7 days = 5.6 TB
- **Weekly backups**: 800 GB × 4 weeks = 3.2 TB
- **Monthly backups**: 800 GB × 12 months = 9.6 TB
- **Recommended retention**: 7 days daily + 4 weeks weekly = ~1 TB

---

## Performance Considerations

### Document Processing:
- **OCR processing time**: 5-15 seconds per document
- **Concurrent processing**: 8-16 documents simultaneously
- **Peak load**: 500-1,000 concurrent users
- **Throughput**: ~100-200 documents/hour per CPU core

### Database Queries:
- **Average query time**: <100ms (with proper indexes)
- **Document search**: <500ms (with full-text indexes)
- **Concurrent connections**: 200-500

### Network Bandwidth:
- **Minimum**: 100 Mbps
- **Recommended**: 1 Gbps
- **For uploads**: 10 MB/s per user (50 documents × 3 MB = 150 MB/user)

---

## Scaling Recommendations

### Vertical Scaling (Single Server):
- **Current**: 16 CPU, 32 GB RAM, 2 TB storage
- **Max recommended**: 32 CPU, 64 GB RAM, 4 TB storage
- **Beyond this**: Consider horizontal scaling

### Horizontal Scaling (Multi-Server):
1. **Load Balancer**: Nginx/HAProxy (2 servers)
2. **Application Servers**: 2-4 backend servers
3. **Database**: MySQL Master-Slave replication
4. **Storage**: Shared storage (NFS/S3) for documents
5. **Cache**: Redis for session management

### Database Scaling:
- **Read Replicas**: 2-3 read replicas for reporting
- **Connection Pooling**: PgBouncer or ProxySQL
- **Partitioning**: Partition documents table by date/organization

---

## Monitoring & Alerts

### Key Metrics to Monitor:
- **CPU Usage**: Alert if >80% for 5 minutes
- **Memory Usage**: Alert if >85% for 5 minutes
- **Disk Usage**: Alert if >80% capacity
- **Database Connections**: Alert if >400 connections
- **Document Processing Queue**: Alert if >100 pending
- **Response Time**: Alert if API >2 seconds

### Recommended Tools:
- **Prometheus + Grafana**: Metrics and dashboards
- **ELK Stack**: Log aggregation
- **New Relic / Datadog**: Application performance monitoring

---

## Backup Strategy

### Daily Backups:
- **Database**: Full backup at 2 AM
- **Documents**: Incremental backup every 6 hours
- **Retention**: 7 days

### Weekly Backups:
- **Full system backup**: Every Sunday at 1 AM
- **Retention**: 4 weeks

### Monthly Backups:
- **Archive backup**: First of each month
- **Retention**: 12 months
- **Offsite storage**: AWS S3 Glacier or similar

---

## Cost Optimization Tips

1. **Use Reserved Instances**: Save 30-40% on cloud costs
2. **Auto-scaling**: Scale down during off-peak hours
3. **Storage Tiering**: Move old documents to cheaper storage (S3 Glacier)
4. **Database Optimization**: Regular cleanup of old logs/data
5. **CDN**: Use CloudFront/CloudFlare for static assets
6. **Compression**: Enable gzip compression for API responses

---

## Minimum Requirements (Not Recommended)

**Absolute minimum for 5,000 users:**
- CPU: 8 cores
- RAM: 16 GB
- Storage: 1 TB
- **Warning**: Will experience performance issues during peak loads

---

## Summary

### Small Business (5 Concurrent Users, 200 Employees):
- **CPU**: 4-8 cores
- **RAM**: 8-16 GB
- **Storage**: 50-100 GB SSD
- **Cost**: ~$40-130/month (cloud)
- **Performance**: Excellent for small business
- **Documents**: 1,000 documents (2 GB storage)

### Enterprise (5,000 Users, 250,000 Documents):
- **CPU**: 16 cores (32 vCPUs)
- **RAM**: 32 GB
- **Storage**: 2 TB SSD (NVMe)
- **Cost**: ~$800/month (cloud)
- **Performance**: Excellent for 5,000 users
- **Documents**: 250,000 documents (750 GB storage)

### Alternative (On-Premise):
- **Small Business**: Entry-level server (4-8 cores, 16 GB RAM, 100 GB SSD) - ~$1,000-2,000
- **Enterprise**: Dell PowerEdge R740 or similar (16 cores, 32 GB RAM, 2 TB NVMe) - ~$3,000-5,000

---

## Next Steps

### For Small Business:
1. **Start with t3.large** (2 vCPU, 8 GB RAM) - ~$60/month
2. **Monitor performance** for 1-2 weeks
3. **Scale up** if you grow beyond 500 employees
4. **Simple monitoring** (basic health checks)
5. **Weekly backups** sufficient

### For Enterprise:
1. **Start with Option 1** (Standard Production)
2. **Monitor performance** for 1-2 weeks
3. **Scale up/down** based on actual usage
4. **Implement caching** (Redis) if needed
5. **Consider CDN** for document delivery
6. **Set up monitoring** and alerts
7. **Plan for growth** (10,000+ users)

---

## Quick Reference Table

| Scenario | Users | Documents | CPU | RAM | Storage | Cost/Month |
|----------|-------|-----------|-----|-----|---------|------------|
| **Small Business** | 5 concurrent, 200 total | 1,000 | 4-8 cores | 8-16 GB | 50-100 GB | $40-130 |
| **Medium Business** | 50 concurrent, 1,000 total | 10,000 | 8-12 cores | 16-24 GB | 500 GB | $200-400 |
| **Enterprise** | 500 concurrent, 5,000 total | 250,000 | 16-32 cores | 32-64 GB | 2-3 TB | $800-1,500 |

---

**Last Updated**: 2024
**Scenarios**: 
- Small Business: 5 concurrent users, 200 employees, 1,000 documents (2 GB)
- Enterprise: 5,000 users, 250,000 documents (750 GB)
**Document Processing**: OCR-intensive workload

