CREATE TABLE player (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id VARCHAR(128) NOT NULL COMMENT '用户ID',
    user_name VARCHAR(128) NOT NULL COMMENT '用户名',
    avatar_url VARCHAR(512) NOT NULL COMMENT '头像URL',
    score BIGINT DEFAULT 0 COMMENT '分数',
    glory BIGINT DEFAULT 0 COMMENT '荣耀值',
    ext TEXT COMMENT '扩展字段',
    game_count INT DEFAULT 0 COMMENT  '游戏局数',
    total_payment INT DEFAULT 0 COMMENT '总付费（单位分）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE INDEX idx_user_id (user_id),
    INDEX idx_user_name (user_name(20))
) COMMENT '玩家表';