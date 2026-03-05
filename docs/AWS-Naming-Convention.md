# AWS 리소스 네이밍 컨벤션

## 기본 규칙

- **Prefix**: `psycho-`
- **팀**: Psycho Factory
- **프로덕트**: Psycho Pizza
- **패턴**: `psycho-{resource}-{environment}-{qualifier}`
- **환경 구분**: `prod`, `dev`, `stg`

---

## 네트워크

| 리소스               | 네이밍                                                      |
|-------------------|----------------------------------------------------------|
| VPC               | `psycho-vpc-prod`                                        |
| 퍼블릭 서브넷           | `psycho-subnet-pub-prod-b`, `psycho-subnet-pub-prod-d`   |
| 프라이빗 서브넷          | `psycho-subnet-priv-prod-b`, `psycho-subnet-priv-prod-d` |
| Internet Gateway  | `psycho-igw-prod`                                        |
| NAT Gateway       | `psycho-natgw-prod`                                      |
| 라우팅 테이블 (퍼블릭)     | `psycho-rtb-pub-prod`                                    |
| 라우팅 테이블 (프라이빗)    | `psycho-rtb-priv-prod`                                   |
| Elastic IP (NAT용) | `psycho-eip-natgw-prod`                                  |

## 보안

| 리소스       | 네이밍                  |
|-----------|----------------------|
| ALB 보안 그룹 | `psycho-sg-alb-prod` |
| EC2 보안 그룹 | `psycho-sg-ec2-prod` |
| RDS 보안 그룹 | `psycho-sg-rds-prod` |

## 컴퓨팅

| 리소스                | 네이밍                          |
|--------------------|------------------------------|
| ALB                | `psycho-alb-prod`            |
| 타겟 그룹 1            | `psycho-tg-1-prod`           |
| 타겟 그룹 2            | `psycho-tg-2-prod`           |
| Launch Template    | `psycho-lt-prod`             |
| Auto Scaling Group | `psycho-asg-prod`            |
| Golden AMI         | `psycho-ami-base-{YYYYMMDD}` |

## 데이터

| 리소스             | 네이밍                    |
|-----------------|------------------------|
| RDS 인스턴스        | `psycho-rds-prod`      |
| DB Subnet Group | `psycho-dbsubnet-prod` |

## 배포

| 리소스                         | 네이밍                            |
|-----------------------------|--------------------------------|
| S3 버킷 (아티팩트)                | `psycho-deploy-artifacts-prod` |
| CodeDeploy Application      | `psycho-codedeploy-app-prod`   |
| CodeDeploy Deployment Group | `psycho-codedeploy-dg-prod`    |

## 태그 공통

모든 리소스에 다음 태그를 부착:

| Key           | Value                                      |
|---------------|--------------------------------------------|
| `Name`        | 실제 이름과 동일하게                                |
| `Team`        | `psycho-factory`                           |
| `Product`     | `psycho-pizza`                             |
| `Environment` | `prod` / `dev` / `stg`                     |
| `ManagedBy`   | `manual` / `codedeploy` / `github-actions` |
