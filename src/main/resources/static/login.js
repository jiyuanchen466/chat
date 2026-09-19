document.addEventListener('DOMContentLoaded', () => {
    // 1. 处理 Tab 切换
    const tabs = document.querySelectorAll('.tab');
    const forms = document.querySelectorAll('.form');

    tabs.forEach(tab => {
        tab.addEventListener('click', () => {
            tabs.forEach(t => t.classList.remove('active'));
            forms.forEach(f => f.classList.remove('active'));

            tab.classList.add('active');
            const targetId = tab.getAttribute('data-target');
            document.getElementById(targetId).classList.add('active');
        });
    });

    // 2. 登录逻辑
    const loginForm = document.getElementById('login-form');
    loginForm.addEventListener('submit', async(e) => {
        e.preventDefault();
        const username = document.getElementById('login-username').value.trim();
        const password = document.getElementById('login-password').value;
        const btn = loginForm.querySelector('.btn-primary');

        if (!username || !password) return alert('请完整填写用户名和密码！');

        // 按钮加载状态
        const originalText = btn.innerText;
        btn.innerText = '登录中...';
        btn.classList.add('loading');

        try {
            const response = await fetch('/api/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, password })
            });
            const result = await response.json();

            if (response.ok && result.code === 200) {
                localStorage.setItem('token', result.data.token);
                localStorage.setItem('userId', result.data.userId);
                localStorage.setItem('nickname', result.data.nickname);
                localStorage.setItem('avatar', result.data.avatar);
                localStorage.setItem('username', username);
                alert('登录成功！正在进入聊天室...');
                // 等下一步写完聊天室页面后解开注释
                window.location.href = 'chat.html';
            } else {
                alert(result.message || '登录失败，请检查账号密码');
            }
        } catch (error) {
            console.error('登录报错:', error);
            alert('网络异常，请确认后端服务是否已启动');
        } finally {
            btn.innerText = originalText;
            btn.classList.remove('loading');
        }
    });

    // 3. 注册逻辑
    // 实时过滤中文字符（当用户输入时触发）
    const regUsernameInput = document.getElementById('reg-username');
    regUsernameInput.addEventListener('input', (e) => {
        // 将非英文、数字、下划线的字符直接替换为空
        e.target.value = e.target.value.replace(/[^a-zA-Z0-9_]/g, '');
    });
    const registerForm = document.getElementById('register-form');
    registerForm.addEventListener('submit', async(e) => {
        e.preventDefault();
        const username = document.getElementById('reg-username').value.trim();
        const nickname = document.getElementById('reg-nickname').value.trim();
        const password = document.getElementById('reg-password').value;
        const btn = registerForm.querySelector('.btn-primary');
        // === 新增：正则校验 ===
        const usernameRegex = /^[a-zA-Z0-9_]{4,20}$/;
        if (!usernameRegex.test(username)) {
            return alert('用户名只能包含英文、数字、下划线，且长度为4-20位！');
        }
        if (!username || !nickname || !password) return alert('请完整填写注册信息！');

        const originalText = btn.innerText;
        btn.innerText = '创建中...';
        btn.classList.add('loading');

        try {
            const response = await fetch('/api/auth/register', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, nickname, password })
            });
            const result = await response.json();

            if (response.ok && result.code === 200) {
                alert('注册成功！请切换到登录标签进行登录');
                document.querySelector('.tab[data-target="login-form"]').click();
                // 清空注册表单
                registerForm.reset();
            } else {
                alert(result.message || '注册失败，用户名可能已被占用');
            }
        } catch (error) {
            console.error('注册报错:', error);
            alert('网络异常，请确认后端服务是否已启动');
        } finally {
            btn.innerText = originalText;
            btn.classList.remove('loading');
        }
    });
});