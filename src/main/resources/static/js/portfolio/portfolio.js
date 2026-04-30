const API_BASE = '/api/v1/interview';

async function evaluatePortfolio() {
    const url = document.getElementById('portfolioUrl').value.trim();
    if (!url) { showToast('URL을 입력해주세요!', true); return; }
    if (!url.startsWith('http')) { showToast('올바른 URL을 입력해주세요!', true); return; }

    openModal();

    try {
        const res = await fetch(`${API_BASE}/portfolio/evaluate?portfolioUrl=${encodeURIComponent(url)}`, {
            method: 'POST'
        });
        if (!res.ok) throw new Error();
        const feedback = await res.text();
        showResult(url, feedback);
    } catch (e) {
        closeModal();
        showToast('평가 중 오류가 발생했습니다. 다시 시도해주세요.', true);
    }
}

function openModal() {
    document.getElementById('modalLoading').style.display = 'flex';
    document.getElementById('modalResult').style.display = 'none';
    document.getElementById('modalOverlay').classList.add('active');
}

function showResult(url, feedback) {
    document.getElementById('modalLoading').style.display = 'none';
    document.getElementById('resultUrl').textContent = url;
    document.getElementById('feedbackText').textContent = feedback;
    document.getElementById('modalResult').style.display = 'block';
}

function closeModal() {
    document.getElementById('modalOverlay').classList.remove('active');
}

// Enter 키로 제출
document.getElementById('portfolioUrl').addEventListener('keydown', e => {
    if (e.key === 'Enter') evaluatePortfolio();
});

// 모달 외부 클릭 시 닫기
document.getElementById('modalOverlay').addEventListener('click', function(e) {
    if (e.target === this) closeModal();
});