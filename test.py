import mysql.connector
from mysql.connector import Error

def create_database_and_table():
    connection = None
    try:
        # 连接MySQL服务器
        print("正在尝试连接到MySQL服务器...")
        connection = mysql.connector.connect(
            host='101.126.150.11',
            port=3306,
            user='yyyjsz',
            password='e6aDarNPvwdhfq9'
        )

        if connection.is_connected():
            cursor = connection.cursor()
            
            # 创建数据库
            cursor.execute("CREATE DATABASE IF NOT EXISTS star")
            cursor.execute("USE star")
            
            # 创建表
            create_table_query = """
            CREATE TABLE IF NOT EXISTS player (
                id INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
                user_id VARCHAR(128) NOT NULL COMMENT '用户ID',
                user_name VARCHAR(128) NOT NULL COMMENT '用户名',
                avatar_url VARCHAR(512) NOT NULL COMMENT '头像URL',
                score BIGINT DEFAULT 0 COMMENT '分数',
                glory BIGINT DEFAULT 0 COMMENT '荣耀值',
                ext TEXT COMMENT '扩展字段',
                game_count INT DEFAULT 0 COMMENT '游戏局数',
                total_payment INT DEFAULT 0 COMMENT '总付费（单位分）',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                UNIQUE INDEX idx_user_id (user_id),
                INDEX idx_user_name (user_name(20))
            ) COMMENT '玩家表';
            """
            cursor.execute(create_table_query)
            print("数据库和表创建成功！")

    except Error as e:
        print(f"Error: {e}")
    finally:
        if connection is not None and connection.is_connected():
            cursor.close()
            connection.close()
            print("MySQL连接已关闭")

if __name__ == "__main__":
    create_database_and_table()
