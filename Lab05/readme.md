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

## Шаг 3. Создание виртуальной машины для подключения к базе данных

Создаю инстанс EC2 в публичной подсети VPC, группа безопасности `web-security-group`, при инициализации устанавливаю MySQL клиент, чтобы упростить подключение к базе данных RDS:
```
#!/bin/bash
dnf update -y
dnf install -y mariadb105
```

![image](screenshots/Screenshot_7.png)

## Шаг 4. Подключение к базе данных и выполнение базовых операций

1. Подключаюсь к инстансу EC2:
```
ssh -i key.pem ec2-user@3.70.176.239
```

![image](screenshots/Screenshot_8.png)

2. Подключаюсь к базе данных RDS с помощью MySQL клиента:
```
bash mysql -h project-rds-mysql-prod.czy2ggkisad2.eu-central-1.rds.amazonaws.com -u admin -p
```

![image](screenshots/Screenshot_9.png)

Создаю таблицу:

```sql
CREATE TABLE categories (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL
);

CREATE TABLE todos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    category_id INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    FOREIGN KEY (category_id) REFERENCES categories(id)
);
```
![image](screenshots/Screenshot_10.png)

Вношу в базу тестовые данные:
```sql
INSERT INTO categories (name) VALUES
('Work'),
('Study'),
('Home');

INSERT INTO todos (title, category_id, status) VALUES
('Finish AWS lab', 2, 'in_progress'),
('Clean the room', 3, 'todo'),
('Prepare presentation', 1, 'done'),
('Watch lecture', 2, 'todo'),
('Pay bills', 3, 'done');
```

![image](screenshots/Screenshot_11.png)

Выполняю несколько запросов на выборку данных:
```sql
SELECT * FROM categories;

SELECT * FROM todos;

SELECT 
    todos.id,
    todos.title,
    todos.status,
    categories.name AS category
FROM todos
JOIN categories ON todos.category_id = categories.id;

SELECT t.title, t.status
FROM todos t
JOIN categories c ON t.category_id = c.id
WHERE c.name = 'Study';
```
![image](screenshots/Screenshot_12.png)

![image](screenshots/Screenshot_13.png)

## Шаг 5. Создание Read Replica
Перехожу в `Databases` -> `Actions` -> `Create read replica`

![image](screenshots/Screenshot_14.png)

Создаю реплику со следующей конфигурацией:

- DB instance identifier: project-rds-mysql-read-replica
- Instance class: db.t3.micro
- Storage type: General Purpose SSD (gp3)
- Enable Enhanced monitoring: Unchecked
- Public access: No
- VPC security groups: db-mysql-security-group

![image](screenshots/Screenshot_15.png)

Подключаюсь к реплике и выполняю несколько запросов:

![image](screenshots/Screenshot_16.png)

> Какие данные вы видите? Объясните почему.

Реплика получает копию данных от primary через механизм репликации, соотвественно данные те же что в основной бд.

![image](screenshots/Screenshot_17.png)

> Получилось ли выполнить запись на Read Replica? Почему?

Реплика только read-only, вставлять данные нельзя, только читать.

Перехожу на основную бд, вставляю следующую строку:

![image](screenshots/Screenshot_18.png)

Возвращаюсь к реплике и проверяю отобразилась ли новая запись:

![image](screenshots/Screenshot_19.png)

> Отобразилась ли новая запись на реплике? Объясните почему.

Новая запись появилась, так как реплика периодически копирует изменения с primary, поэтому новые данные появляются автоматически.

> Объясните, зачем нужны Read Replicas и в каких сценариях их использование будет полезным.

Read Replicas нужны для разгрузки основной базы данных и масштабирования чтения.
Они полезны, когда много запросов SELECT, при аналитике, отчётах, дашбордах, поиске и любых операциях, где нужна высокая скорость чтения без нагрузки на primary.

## Шаг 6a. Развертывание CRUD приложения
1. Устанавливаю зависимости в инстанс (java, jdk, mysqldriver), создаю там файлы приложения и копирую в них содержимое

```java
import java.io.FileInputStream;
import java.sql.*;
import java.util.Properties;

public class Main {

    private static String masterUrl;
    private static String replicaUrl;
    private static String user;
    private static String pass;

    public static void main(String[] args) throws Exception {

        loadConfig();

        createCategory("NewCategory");
        readCategories();
        updateCategory(1, "UpdatedName");
        deleteCategory(2);
        readCategoriesFromReplica();
    }

    private static void loadConfig() throws Exception {
        Properties p = new Properties();
        p.load(new FileInputStream("config.properties"));

        masterUrl = p.getProperty("master.url");
        replicaUrl = p.getProperty("replica.url");
        user = p.getProperty("db.user");
        pass = p.getProperty("db.pass");
    }

    private static void createCategory(String name) {
        try (Connection conn = DriverManager.getConnection(masterUrl, user, pass)) {
            PreparedStatement ps =
                conn.prepareStatement("INSERT INTO categories (name) VALUES (?)");
            ps.setString(1, name);
            ps.executeUpdate();
            System.out.println("CREATE OK");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static void readCategories() {
        try (Connection conn = DriverManager.getConnection(masterUrl, user, pass)) {
            ResultSet rs = conn.prepareStatement("SELECT * FROM categories").executeQuery();
            System.out.println("READ from MASTER:");
            while (rs.next()) {
                System.out.println(rs.getInt("id") + " | " + rs.getString("name"));
            }
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
    }

    private static void updateCategory(int id, String newName) {
        try (Connection conn = DriverManager.getConnection(masterUrl, user, pass)) {
            PreparedStatement ps =
                conn.prepareStatement("UPDATE categories SET name=? WHERE id=?");
            ps.setString(1, newName);
            ps.setInt(2, id);
            ps.executeUpdate();
            System.out.println("UPDATE OK");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static void deleteCategory(int id) {
        try (Connection conn = DriverManager.getConnection(masterUrl, user, pass)) {
            PreparedStatement ps =
                conn.prepareStatement("DELETE FROM categories WHERE id=?");
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("DELETE OK");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static void readCategoriesFromReplica() {
        try (Connection conn = DriverManager.getConnection(replicaUrl, user, pass)) {
            ResultSet rs = conn.prepareStatement("SELECT * FROM categories").executeQuery();
            System.out.println("READ from REPLICA:");
            while (rs.next()) {
                System.out.println(rs.getInt("id") + " | " + rs.getString("name"));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}

```

`main` метод:
```java
    public static void main(String[] args) throws Exception {

        loadConfig();

        createCategory("NewCategory");
        readCategories();
        updateCategory(1, "UpdatedName");
        deleteCategory(2);
        readCategoriesFromReplica();
    }
```

Методы вызываются по очереди и выполняют соответствующие действия. В случае успеха будет выводиться сообщение в консоли.

2. Компилирую и выполняю приложение в инстансе:

![image](screenshots/Screenshot_20.png)

Видим логирование каждой операции, а также исключение, вызванной тем что в todos есть строки, у которых category_id = 1, MySQL не даёт удалить категорию, пока на неё ссылаются todos (FK constraint). Тем не менее это не является ошибкой и метод работает исправно.