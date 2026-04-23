// ─── 공통 토스트 알림 ───
function showToast(message, isError = false) {
    const toast = document.getElementById('toast');
    if(!toast) return;
    toast.innerText = message;
    isError ? toast.classList.add('error') : toast.classList.remove('error');
    toast.classList.add('show');
    setTimeout(() => toast.classList.remove('show'), 3000);
}

// ─── 공통 Fetch (토큰 만료 자동 처리) ───
async function customFetch(url, options = {}) {
    let response = await fetch(url, options);

    if (response.status === 401) {
        console.log("Access Token 만료 감지! 재발급 시도...");
        const reissueResponse = await fetch('/api/v1/auth/reissue', { method: 'POST' });

        if (reissueResponse.ok) {
            console.log("토큰 재발급 성공, 원래 요청 재시도");
            response = await fetch(url, options);
        } else {
            showToast('세션이 만료되었습니다. 다시 로그인해주세요.', true);
            setTimeout(() => { window.location.href = '/api/v1/auth/loginP'; }, 1500);
            throw new Error("세션 만료");
        }
    }
    return response;
}