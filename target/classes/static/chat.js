// ==================== 全局变量 ====================
let currentUser = null;
let targetUser = null;
let ws = null;
let currentSessionId = null;

// ==================== 初始化 ====================
document.addEventListener('DOMContentLoaded', () => {
    // ========== 手机侧边栏切换 ==========
    const toggleBtn = document.getElementById('toggle-sidebar');
    const sidebar = document.getElementById('sidebar');
    const sidebarMask = document.getElementById('sidebar-mask');

    function openSidebar() {
        sidebar.classList.add('show');
        sidebarMask.classList.add('show');
    }

    function closeSidebar() {
        sidebar.classList.remove('show');
        sidebarMask.classList.remove('show');
    }

    if (toggleBtn) {
        toggleBtn.addEventListener('click', () => {
            if (sidebar.classList.contains('show')) closeSidebar();
            else openSidebar();
        });
    }
    if (sidebarMask) {
        sidebarMask.addEventListener('click', closeSidebar);
    }
    const userId = localStorage.getItem('userId');
    const nickname = localStorage.getItem('nickname');
    const avatar = localStorage.getItem('avatar');

    if (!userId) {
        alert('请先登录！');
        window.location.href = 'login.html';
        return;
    }

    currentUser = { userId, nickname, avatar };
    document.getElementById('my-nickname').innerText = nickname || '未知用户';
    document.getElementById('my-avatar').src = avatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=' + userId;

    // 加载数据
    loadFriendList();
    loadRequestBadge();

    // 轮询刷新好友列表和红点
    setInterval(() => {
        loadFriendList();
        loadRequestBadge();
    }, 5000);

    // WebSocket 连接
    initWebSocket(userId);

    // 发送按钮 / 回车发送
    document.getElementById('send-btn').addEventListener('click', sendMessage);
    document.getElementById('message-input').addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
            e.preventDefault();
            sendMessage();
        }
    });

    // 表情面板
    const emojiBtn = document.getElementById('emoji-btn');
    const emojiPanel = document.getElementById('emoji-panel');
    const msgInput = document.getElementById('message-input');

    if (emojiBtn) {
        emojiBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            emojiPanel.classList.toggle('show');
        });
    }
    if (emojiPanel) {
        emojiPanel.querySelectorAll('span').forEach(span => {
            span.addEventListener('click', () => {
                msgInput.value += span.innerText;
                msgInput.focus();
            });
        });
        document.addEventListener('click', (e) => {
            if (!emojiPanel.contains(e.target) && e.target !== emojiBtn) {
                emojiPanel.classList.remove('show');
            }
        });
    }

    // 加好友弹窗
    const addBtn = document.getElementById('add-friend-btn');
    if (addBtn) {
        addBtn.addEventListener('click', () => {
            document.getElementById('add-friend-modal').classList.add('show');
            document.getElementById('search-results').innerHTML = '';
            document.getElementById('search-input').value = '';
        });
    }
    const searchBtn = document.getElementById('search-btn');
    if (searchBtn) searchBtn.addEventListener('click', doSearch);
    const searchInput = document.getElementById('search-input');
    if (searchInput) {
        searchInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') doSearch();
        });
    }

    // 好友申请弹窗
    const reqBtn = document.getElementById('friend-request-btn');
    if (reqBtn) {
        reqBtn.addEventListener('click', async() => {
            document.getElementById('friend-request-modal').classList.add('show');
            await renderRequestList();
        });
    }

    // 关闭弹窗
    document.querySelectorAll('.close-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const modalId = btn.getAttribute('data-modal');
            document.getElementById(modalId).classList.remove('show');
        });
    });
    document.querySelectorAll('.modal').forEach(m => {
        m.addEventListener('click', (e) => {
            if (e.target === m) m.classList.remove('show');
        });
    });
});

// ==================== WebSocket ====================
function initWebSocket(userId) {
    const protocol = window.location.protocol === 'https:' ? 'wss://' : 'ws://';
    const wsUrl = `${protocol}${window.location.host}/ws/chat?userId=${userId}`;
    ws = new WebSocket(wsUrl);

    ws.onopen = () => console.log('✅ WebSocket 连接成功');

    ws.onmessage = (event) => {
        try {
            const msg = JSON.parse(event.data);
            console.log('📥 收到消息:', msg);

            // 自己发的跳过（本地已渲染）
            if (msg.fromUserId === currentUser.userId) return;

            // 不是发给我的跳过
            if (msg.toUserId !== currentUser.userId) return;

            // 只有当前正打开该好友的会话才渲染
            if (targetUser && targetUser.userId === msg.fromUserId) {
                appendMessage(msg, false);

                // 清未读
                fetch('/api/unread/read', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ userId: currentUser.userId, fromUserId: msg.fromUserId })
                });
            }

            // 刷新好友列表（红点会更新）
            loadFriendList();
        } catch (e) {
            console.error('解析消息失败:', e);
        }
    };

    ws.onclose = () => {
        console.log('⚠️ WebSocket 断开，3秒后重连...');
        setTimeout(() => initWebSocket(userId), 3000);
    };

    ws.onerror = (error) => console.error('WebSocket 错误:', error);
}

// ==================== 发送消息 ====================
function sendMessage() {
    console.log('=== sendMessage 被调用 ===');
    const input = document.getElementById('message-input');
    const content = input.value.trim();

    if (!content) {
        console.log('消息为空');
        return;
    }
    if (!targetUser) {
        alert('请先点击左侧好友选择一个聊天对象');
        return;
    }
    if (!ws || ws.readyState !== WebSocket.OPEN) {
        alert('WebSocket 未连接，请刷新页面');
        return;
    }

    const sessionId = [currentUser.userId, targetUser.userId].sort().join("_");

    const msgData = {
        fromUserId: currentUser.userId,
        toUserId: targetUser.userId,
        content: content,
        type: "1",
        sessionId: sessionId
    };

    console.log('📤 发送:', msgData);

    // 1. 发送到后端
    ws.send(JSON.stringify(msgData));

    // 2. 立刻本地渲染（关键！不依赖后端回发）
    appendMessage(msgData, true);

    // 3. 清空输入框
    input.value = '';
    input.focus();

    console.log('✅ 已发送并本地渲染');
}

// ==================== 渲染消息 ====================
function appendMessage(msg, isSelf) {
    const area = document.getElementById('message-area');
    if (!area) {
        console.error('❌ 找不到 message-area');
        return;
    }

    const isMyMessage = isSelf || msg.fromUserId === currentUser.userId;

    const msgDiv = document.createElement('div');
    msgDiv.className = `message ${isMyMessage ? 'self' : 'other'}`;

    const now = new Date();
    const timeStr = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}`;

    msgDiv.innerHTML = `
        <div class="bubble">${escapeHtml(msg.content)}</div>
        <div class="meta">${timeStr}</div>
    `;
    area.appendChild(msgDiv);
    area.scrollTop = area.scrollHeight;

    console.log('✅ 消息已渲染:', msg.content);
}

function escapeHtml(str) {
    if (!str) return '';
    return String(str).replace(/[&<>"']/g, (m) => ({
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#39;'
    }[m]));
}

// ==================== 选择聊天对象 ====================
async function selectUser(user, element) {
    targetUser = user;
    currentSessionId = [currentUser.userId, user.userId].sort().join("_");

    document.querySelectorAll('.user-item').forEach(el => el.classList.remove('active'));
    element.classList.add('active');

    document.getElementById('chat-header').innerHTML = `<h2>正在与 ${user.nickname || user.username} 聊天</h2>`;
    document.getElementById('message-input').disabled = false;
    document.getElementById('send-btn').disabled = false;
    document.getElementById('message-input').focus();

    // 清空消息区
    document.getElementById('message-area').innerHTML = '';

    // 拉历史消息
    await loadHistory(currentSessionId);

    // 清未读
    try {
        await fetch('/api/unread/read', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ userId: currentUser.userId, fromUserId: user.userId })
        });
        loadFriendList();
    } catch (e) { console.error(e); }
    // 手机上选中好友后自动收起侧边栏
    if (window.innerWidth <= 768) {
        closeSidebar();
    }
}

// 拉取历史消息
async function loadHistory(sessionId) {
    try {
        const res = await fetch(`/api/messages/history?sessionId=${sessionId}&page=0&size=50`);
        const result = await res.json();
        if (result.code === 200 && result.data.length > 0) {
            const area = document.getElementById('message-area');
            area.innerHTML = '';
            result.data.forEach(m => {
                const isSelf = m.fromUserId === currentUser.userId;
                const msgDiv = document.createElement('div');
                msgDiv.className = `message ${isSelf ? 'self' : 'other'}`;
                const time = new Date(m.createTime);
                const timeStr = `${time.getHours().toString().padStart(2, '0')}:${time.getMinutes().toString().padStart(2, '0')}`;
                msgDiv.innerHTML = `
                    <div class="bubble">${escapeHtml(m.content)}</div>
                    <div class="meta">${timeStr}</div>
                `;
                area.appendChild(msgDiv);
            });
            area.scrollTop = area.scrollHeight;
        }
    } catch (e) { console.error('拉取历史消息失败:', e); }
}

// ==================== 好友列表 ====================
async function loadFriendList() {
    try {
        const [friendsRes, unreadRes] = await Promise.all([
            fetch(`/api/friends?userId=${currentUser.userId}`),
            fetch(`/api/unread?userId=${currentUser.userId}`)
        ]);
        const friendsData = await friendsRes.json();
        const unreadData = await unreadRes.json();
        if (friendsData.code === 200) {
            renderFriendList(friendsData.data, unreadData.data || {});
        }
    } catch (error) {
        console.error('获取好友列表失败:', error);
    }
}

function renderFriendList(users, unreadMap) {
    const listContainer = document.getElementById('user-list');
    if (!listContainer) return;
    listContainer.innerHTML = '';

    if (!users || users.length === 0) {
        listContainer.innerHTML = '<div class="empty-tip">还没有好友<br>点击上方"加好友"添加</div>';
        return;
    }

    users.forEach(user => {
                const unread = unreadMap[user.userId] || 0;
                const item = document.createElement('div');
                item.className = 'user-item';
                if (targetUser && targetUser.userId === user.userId) {
                    item.classList.add('active');
                }
                item.innerHTML = `
            <img src="${user.avatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=' + user.username}" alt="头像">
            <div class="name">${user.nickname || user.username}</div>
            ${unread > 0 ? `<span class="user-badge">${unread > 99 ? '99+' : unread}</span>` : ''}
        `;
        item.onclick = () => selectUser(user, item);
        listContainer.appendChild(item);
    });
}

// ==================== 加好友 ====================
async function doSearch() {
    const keyword = document.getElementById('search-input').value.trim();
    if (!keyword) return;
    const resultsEl = document.getElementById('search-results');

    try {
        const res = await fetch(`/api/users/search?keyword=${encodeURIComponent(keyword)}&currentUserId=${currentUser.userId}`);
        const result = await res.json();
        if (result.code !== 200) return;

        if (result.data.length === 0) {
            resultsEl.innerHTML = '<div class="empty-tip">没有找到相关用户</div>';
            return;
        }

        resultsEl.innerHTML = '';
        result.data.forEach(u => {
            const item = document.createElement('div');
            item.className = 'result-item';
            item.innerHTML = `
                <img src="${u.avatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=' + u.username}">
                <div class="info">
                    <div class="name">${u.nickname || u.username}</div>
                    <div class="sub">@${u.username}</div>
                </div>
                <button class="action-btn add" data-uid="${u.userId}">添加</button>
            `;
            item.querySelector('.action-btn').addEventListener('click', (ev) => {
                sendFriendRequest(u.userId, ev.target);
            });
            resultsEl.appendChild(item);
        });
    } catch (e) {
        console.error(e);
    }
}

async function sendFriendRequest(toUserId, btn) {
    btn.disabled = true;
    btn.innerText = '发送中...';
    try {
        const res = await fetch('/api/friends/request', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ fromUserId: currentUser.userId, toUserId })
        });
        const result = await res.json();
        if (result.code === 200) {
            btn.innerText = '已发送';
        } else {
            alert(result.message);
            btn.disabled = false;
            btn.innerText = '添加';
        }
    } catch (e) {
        alert('网络错误');
        btn.disabled = false;
        btn.innerText = '添加';
    }
}

// ==================== 好友申请 ====================
async function loadRequestBadge() {
    try {
        const res = await fetch(`/api/friends/requests?userId=${currentUser.userId}`);
        const result = await res.json();
        if (result.code === 200) {
            const count = result.data.length;
            const badge = document.getElementById('request-badge');
            if (!badge) return;
            if (count > 0) {
                badge.innerText = count;
                badge.style.display = 'inline-block';
            } else {
                badge.style.display = 'none';
            }
        }
    } catch (e) { console.error(e); }
}

async function renderRequestList() {
    const listEl = document.getElementById('request-list');
    if (!listEl) return;
    try {
        const res = await fetch(`/api/friends/requests?userId=${currentUser.userId}`);
        const result = await res.json();
        if (result.code !== 200) return;

        if (result.data.length === 0) {
            listEl.innerHTML = '<div class="empty-tip">暂无新的好友申请</div>';
            return;
        }

        listEl.innerHTML = '';
        result.data.forEach(req => {
            const item = document.createElement('div');
            item.className = 'request-item';
            item.innerHTML = `
                <img src="${req.avatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=' + req.username}">
                <div class="info">
                    <div class="name">${req.nickname || req.username}</div>
                    <div class="sub">@${req.username} 请求加你为好友</div>
                </div>
                <button class="action-btn accept">同意</button>
                <button class="action-btn reject">拒绝</button>
            `;
            item.querySelector('.accept').addEventListener('click', () => handleRequest(req.userId, 'accept', item));
            item.querySelector('.reject').addEventListener('click', () => handleRequest(req.userId, 'reject', item));
            listEl.appendChild(item);
        });
    } catch (e) { console.error(e); }
}

async function handleRequest(requesterId, action, itemEl) {
    try {
        const res = await fetch(`/api/friends/${action}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ userId: currentUser.userId, requesterId })
        });
        const result = await res.json();
        if (result.code === 200) {
            itemEl.remove();
            if (action === 'accept') loadFriendList();
            loadRequestBadge();
        } else {
            alert(result.message);
        }
    } catch (e) { alert('网络错误'); }
}