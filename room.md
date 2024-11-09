房间系统
在弹幕游戏中，一个玩家只能加入一个房间，当加入下一个房间后，玩家得从上一个房间退出。

房间数据结构：
1. anchorOpenID string 主播ID，全局唯一
2. playerList list<string> 玩家IDs
3. 创建时间

userID -> anchorOpenID 映射

数据存储使用redis

提供四个接口：
1. 创建房间：
传入主播ID： anchorOpenID 这个全局唯一
返回房间信息

当房间已存在时，返回房间信息即可，不需要重新创建

2. 关闭房间：
传入主播ID： anchorOpenID 这个全局唯一
当主播下播时调用该接口
删除房间
返回被删除的房间信息

3. 玩家加入房间（需要原子，使用lua脚本）
入参： 玩家userID和主播anchorOpenID

如果玩家已经加入别的房间，则先退出那个房间
然后加入目标房间

返回： 玩家退出的房间anchorOpenID（如有）

4. 获取房间信息

