let currentUser = null;

document.addEventListener('DOMContentLoaded', () => {
    const userId = localStorage.getItem('userId');
    if (!userId) {
        alert('请先登录！');
        window.location.href = 'login.html';
        return;
    }

    currentUser = {
        userId,
        nickname: localStorage.getItem('nickname'),
        avatar: localStorage.getItem('avatar')
    };



    // 拉取最新用户信息，覆盖上面的默认值
    loadUserProfile();

    // 绑定头像上传
    document.getElementById('avatar-upload').addEventListener('change', handleAvatarUpload);

    // 绑定保存
    document.getElementById('save-btn').addEventListener('click', handleSave);
});

// 拉取用户完整信息
async function loadUserProfile() {
    try {
        const res = await fetch(`/api/users/${currentUser.userId}`);
        const result = await res.json();
        if (result.code === 200) {
            const u = result.data;

            // 填充用户名（关键）
            document.getElementById('username').value = u.username || '';

            // 填充昵称
            document.getElementById('nickname').value = u.nickname || '';

            // 填充签名
            document.getElementById('signature').value = u.signature || '';

            // 填充邮箱
            document.getElementById('email').value = u.email || '';

            // 填充头像
            if (u.avatar) {
                document.getElementById('preview-avatar').src = u.avatar;
                currentUser.avatar = u.avatar;
            }

            // 填充性别
            if (u.gender !== null && u.gender !== undefined) {
                const genderRadio = document.querySelector(`input[name="gender"][value="${u.gender}"]`);
                if (genderRadio) genderRadio.checked = true;
            }
        } else {
            alert('获取用户信息失败：' + result.message);
        }
    } catch (e) {
        console.error('拉取用户信息失败', e);
    }
}

// 处理头像上传
async function handleAvatarUpload(e) {
    const file = e.target.files[0];
    if (!file) return;

    // 本地预览
    const reader = new FileReader();
    reader.onload = (ev) => {
        document.getElementById('preview-avatar').src = ev.target.result;
    };
    reader.readAsDataURL(file);

    // 上传到后端
    const formData = new FormData();
    formData.append('file', file);

    try {
        const res = await fetch('/api/files/upload', {
            method: 'POST',
            body: formData
        });
        const result = await res.json();
        if (result.code === 200) {
            currentUser.avatar = result.data.url;
            document.getElementById('preview-avatar').src = result.data.url;
        } else {
            alert('头像上传失败：' + result.message);
        }
    } catch (err) {
        console.error(err);
        alert('头像上传失败，请检查网络');
    }
}

// 保存资料
async function handleSave() {
    const btn = document.getElementById('save-btn');
    btn.disabled = true;
    btn.innerText = '保存中...';

    const payload = {
        userId: currentUser.userId,
        nickname: document.getElementById('nickname').value.trim(),
        avatar: currentUser.avatar,
        signature: document.getElementById('signature').value.trim(),
        gender: (function() {
            const checked = document.querySelector('input[name="gender"]:checked');
            return checked ? parseInt(checked.value) : 0;
        })(),
        email: document.getElementById('email').value.trim()
    };

    if (!payload.nickname) {
        alert('昵称不能为空');
        btn.disabled = false;
        btn.innerText = '保存修改';
        return;
    }

    try {
        const res = await fetch('/api/users/profile', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const result = await res.json();
        if (result.code === 200) {
            // 更新 localStorage
            localStorage.setItem('nickname', result.data.nickname);
            localStorage.setItem('avatar', result.data.avatar);
            alert('保存成功！');
        } else {
            alert('保存失败：' + result.message);
        }
    } catch (err) {
        console.error(err);
        alert('网络异常');
    } finally {
        btn.disabled = false;
        btn.innerText = '保存修改';
    }
}