import http.server
import socketserver
from http import HTTPStatus
import json
from datetime import datetime, timezone
import urllib.parse
from typing import Dict, List
import threading
import logging

PORT = 8080

# 设置日志
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# 内存存储
countdowns: Dict[int, dict] = {}
current_id = 0
id_lock = threading.Lock()
comments: Dict[int, List[dict]] = {}  # countdown_id -> list of comments

def get_next_id() -> int:
    global current_id
    with id_lock:
        current_id += 1
        return current_id

def sort_countdowns(countdowns_list: List[dict]) -> List[dict]:
    return sorted(
        countdowns_list,
        key=lambda x: (-x['likes'], x['targetDate'])
    )

class CountdownHandler(http.server.SimpleHTTPRequestHandler):
    def send_cors_headers(self):
        # 获取请求的 Origin
        origin = self.headers.get('Origin')
        
        # 允许的域名列表
        allowed_origins = [
            'https://magical-gumption-7294c7.netlify.app',
            'http://localhost:5173'  # 开发环境
        ]
        
        # 如果请求的 Origin 在允许列表中，则返回对应的 Origin
        if origin in allowed_origins:
            self.send_header('Access-Control-Allow-Origin', origin)
        else:
            self.send_header('Access-Control-Allow-Origin', '*')
            
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, PATCH, OPTIONS, DELETE')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type, Accept, Authorization')
        self.send_header('Access-Control-Max-Age', '3600')
        self.send_header('Access-Control-Expose-Headers', 'Content-Type')
        
    def send_json_response(self, data, status=HTTPStatus.OK):
        self.send_response(status)
        self.send_cors_headers()
        self.send_header('Content-Type', 'application/json')
        self.end_headers()
        self.wfile.write(json.dumps(data).encode('utf-8'))

    def do_OPTIONS(self):
        self.send_response(HTTPStatus.NO_CONTENT)
        self.send_cors_headers()
        self.send_header('Access-Control-Max-Age', '86400')
        self.end_headers()

    def get_json_body(self):
        content_length = int(self.headers.get('Content-Length', 0))
        body = self.rfile.read(content_length)
        return json.loads(body)

    def do_GET(self):
        if self.path.startswith('/api/countdowns'):
            if self.path.endswith('/comments'):
                # 获取特定计时器的评论
                try:
                    countdown_id = int(self.path.split('/')[-2])
                    countdown_comments = comments.get(countdown_id, [])
                    self.send_json_response(countdown_comments)
                except ValueError:
                    self.send_error(HTTPStatus.BAD_REQUEST)
                return
            parsed_url = urllib.parse.urlparse(self.path)
            query_params = urllib.parse.parse_qs(parsed_url.query)
            status = query_params.get('status', [None])[0]
            
            now = datetime.now(timezone.utc)
            result = []
            
            for countdown in countdowns.values():
                try:
                    # Parse the target date and ensure it's UTC aware
                    target_str = countdown['targetDate'].replace('Z', '+00:00')
                    target = datetime.fromisoformat(target_str)
                    if target.tzinfo is None:
                        target = target.replace(tzinfo=timezone.utc)
                    
                    logger.debug(f"Comparing target: {target} with now: {now}")
                    
                    if status == 'active' and target <= now:
                        continue
                    if status == 'expired' and target > now:
                        continue

                    # 添加评论数到计时器数据中
                    countdown_with_comments = countdown.copy()
                    countdown_with_comments['commentCount'] = len(comments.get(countdown['id'], []))
                    result.append(countdown_with_comments)
                except Exception as e:
                    logger.error(f"Error processing countdown {countdown['id']}: {e}")
                    continue
            
            self.send_json_response(sort_countdowns(result))
        else:
            self.send_error(HTTPStatus.NOT_FOUND)

    def do_POST(self):
        if self.path.startswith('/api/countdowns'):
            if self.path.endswith('/comments'):
                # 添加评论
                try:
                    countdown_id = int(self.path.split('/')[-2])
                    if countdown_id not in countdowns:
                        self.send_json_response({'error': 'Countdown not found'}, HTTPStatus.NOT_FOUND)
                        return

                    data = self.get_json_body()
                    if not data or 'content' not in data:
                        self.send_json_response({'error': 'Missing comment content'}, HTTPStatus.BAD_REQUEST)
                        return

                    new_comment = {
                        'id': get_next_id(),
                        'content': data['content'],
                        'createdAt': datetime.now(timezone.utc).isoformat(),
                        'countdown_id': countdown_id
                    }

                    if countdown_id not in comments:
                        comments[countdown_id] = []
                    comments[countdown_id].append(new_comment)
                    
                    logger.info(f"Added new comment to countdown {countdown_id}: {new_comment}")
                    self.send_json_response(new_comment)
                except ValueError:
                    self.send_error(HTTPStatus.BAD_REQUEST)
                return
            try:
                data = self.get_json_body()
                
                if not data or 'title' not in data or 'targetDate' not in data:
                    self.send_json_response({'error': 'Missing required fields'}, HTTPStatus.BAD_REQUEST)
                    return
                
                new_countdown = {
                    'id': get_next_id(),
                    'title': data['title'],
                    'targetDate': data['targetDate'],
                    'createdAt': datetime.now(timezone.utc).isoformat(),
                    'likes': 0
                }
                
                countdowns[new_countdown['id']] = new_countdown
                logger.info(f"Created new countdown: {new_countdown}")
                self.send_json_response(new_countdown)
            except json.JSONDecodeError:
                logger.error("Invalid JSON received")
                self.send_json_response({'error': 'Invalid JSON'}, HTTPStatus.BAD_REQUEST)
        else:
            self.send_error(HTTPStatus.NOT_FOUND)

    def do_PATCH(self):
        if self.path.startswith('/api/countdowns/'):
            try:
                countdown_id = int(self.path.split('/')[-2])
                
                if self.path.endswith('/like'):
                    if countdown_id not in countdowns:
                        self.send_json_response({'error': 'Countdown not found'}, HTTPStatus.NOT_FOUND)
                        return
                    
                    countdown = countdowns[countdown_id]
                    countdown['likes'] += 1
                    self.send_json_response(countdown)
                else:
                    self.send_error(HTTPStatus.NOT_FOUND)
            except ValueError:
                self.send_error(HTTPStatus.BAD_REQUEST)
        else:
            self.send_error(HTTPStatus.NOT_FOUND)

    def do_DELETE(self):
        if self.path.startswith('/api/countdowns'):
            try:
                parts = self.path.split('/')
                if len(parts) >= 5 and parts[-2] == 'comments':
                    # 删除评论
                    countdown_id = int(parts[-3])
                    comment_id = int(parts[-1])
                    
                    if countdown_id not in comments:
                        self.send_json_response({'error': 'Countdown not found'}, HTTPStatus.NOT_FOUND)
                        return
                    
                    countdown_comments = comments[countdown_id]
                    comment_index = next((i for i, c in enumerate(countdown_comments) 
                                       if c['id'] == comment_id), -1)
                    
                    if comment_index == -1:
                        self.send_json_response({'error': 'Comment not found'}, HTTPStatus.NOT_FOUND)
                        return
                    
                    deleted_comment = countdown_comments.pop(comment_index)
                    logger.info(f"Deleted comment {comment_id} from countdown {countdown_id}")
                    self.send_json_response(deleted_comment)
                    return
            except ValueError:
                self.send_error(HTTPStatus.BAD_REQUEST)
        self.send_error(HTTPStatus.NOT_FOUND)

    def send_error(self, code, message=None):
        self.send_response(code)
        self.send_cors_headers()  # 添加 CORS 头到错误响应
        self.send_header('Content-Type', 'text/html')
        self.end_headers()
        if message:
            self.wfile.write(message.encode('utf-8'))

if __name__ == '__main__':
    with socketserver.TCPServer(("", PORT), CountdownHandler, False) as httpd:
        print(f"Server started at port {PORT}")
        httpd.allow_reuse_address = True
        httpd.server_bind()
        httpd.server_activate()
        httpd.serve_forever()
