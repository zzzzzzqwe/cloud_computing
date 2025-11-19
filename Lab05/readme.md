# Лабораторная работа 5
# Студент: Gachayev Dmitrii, I2302
# Дата выполнения: 19.11.2025
# Задача

# Цель

---

## Шаг 1. Подготовка среды (VPC/подсети/SG)

1. Создаю VPC через `VPC and more`:

![image](screenshots/Screenshot_1.png)

2. Перехожу в `EC2` -> `Security Groups` и создаю Security Group `web-security-group`

![image](screenshots/Screenshot_2.png)

А также `db-mysql-security-group`:

![image](screenshots/Screenshot_3.png)

Добавляю правило в `web-security-group`:

![image](screenshots/Screenshot_4.png)

## Шаг 2. Развертывание Amazon RDS
> Что такое Subnet Group? И зачем необходимо создавать Subnet Group для базы данных?

Subnet Group - набор приватных подсетей в одной VPC, расположенных в разных Availability Zones. Это необходимо для размещения базы в приватных подсетях и обеспечения отказоустойчивости (база разворачивается в двух подсетях в разных зонах доступности).

Создаю `Subnet Group`:

![image](screenshots/Screenshot_5.png)
 
 Выбираю созданный ранее VPC и добавляю 2 приватные подсети из 2 разных AZ.

 Создаю экземпляр базы данных Amazon RDS со следующей конфигурацией:

- Engine type: MySQL
- Version: MySQL 8.0.42
- Templater: Free tier
- Availability and durability: Single-AZ DB instance deployment
- DB instance identifier: project-rds-mysql-prod
- Master username: admin
- DB instance class: Burstable classes (includes t classes), db.t3.micro

Storage:

- Storage type: General Purpose SSD (gp3)
- Allocated storage: 20 GB
- Enable storage autoscaling: Checked
- Maximum storage threshold: 100 GB

Connectivity:

- Don’t connect to an EC2 compute resource
- Virtual private cloud (VPC) - созданный ранее VPC
- DB subnet group: project-rds-subnet-group1
- Public access: No
- Existing VPC security groups: db-mysql-security-group
- Availability zone: No preference

Additional configuration:

- Initial database name: project_db
- Backup (Enable automated backup): Checked
- Backup (Enable encryption): Unchecked
- Maintanance (Enable auto minor version upgrade): Unchecked

![image](screenshots/Screenshot_6.png)