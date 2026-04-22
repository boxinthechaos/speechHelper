document.addEventListener('DOMContentLoaded', () => {
    const pupils = document.querySelectorAll('.pupil');
    const eyes = document.querySelectorAll('.eye');
    const inputs = document.querySelectorAll('input');

    // ─── 1. 마우스 추적 눈동자 ───
    document.addEventListener('mousemove', (e) => {
        const isInputFocused = Array.from(inputs).some(input => input === document.activeElement);
        if (isInputFocused) return;

        pupils.forEach(pupil => {
            const eye = pupil.parentElement;
            if (eye.classList.contains('eye-closed')) return;

            const eyeRect = eye.getBoundingClientRect();
            const eyeCenterX = eyeRect.left + eyeRect.width / 2;
            const eyeCenterY = eyeRect.top + eyeRect.height / 2;

            const angle = Math.atan2(e.clientY - eyeCenterY, e.clientX - eyeCenterX);
            const distance = 5;
            const pupilX = Math.cos(angle) * distance;
            const pupilY = Math.sin(angle) * distance;

            pupil.style.transform = `translate(${pupilX}px, ${pupilY}px)`;
        });
    });

    // ─── 2. 포커스 시 눈 감기 ───
    inputs.forEach(input => {
        input.addEventListener('focus', () => {
            eyes.forEach(eye => eye.classList.add('eye-closed'));
            pupils.forEach(pupil => { pupil.style.transform = 'translate(0px, 0px)'; });
        });
        input.addEventListener('blur', () => {
            eyes.forEach(eye => eye.classList.remove('eye-closed'));
        });
    });

    // ─── 3. 실시간 유효성 검사 ───

    // 이름: 2자 이상
    document.getElementById('name').addEventListener('input', function () {
        const val = this.value.trim();
        const hint = document.getElementById('hint-name');
        if (val.length === 0) {
            setFieldState(this, hint, '', '');
        } else if (val.length < 2) {
            setFieldState(this, hint, '이름은 2자 이상이어야 합니다.', 'invalid');
        } else {
            setFieldState(this, hint, '✓ 좋아요!', 'valid');
        }
    });

    // 이메일: 형식 검사
    document.getElementById('email').addEventListener('input', function () {
        const val = this.value.trim();
        const hint = document.getElementById('hint-email');
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (val.length === 0) {
            setFieldState(this, hint, '', '');
        } else if (!emailRegex.test(val)) {
            setFieldState(this, hint, '올바른 이메일 형식을 입력해주세요.', 'invalid');
        } else {
            setFieldState(this, hint, '✓ 올바른 이메일 형식입니다.', 'valid');
        }
    });

    // 비밀번호: 강도 측정
    document.getElementById('password').addEventListener('input', function () {
        const val = this.value;
        const hint = document.getElementById('hint-password');
        const bar = document.getElementById('strength-bar');

        if (val.length === 0) {
            bar.style.width = '0%';
            bar.style.backgroundColor = 'transparent';
            setFieldState(this, hint, '', '');
            return;
        }

        const strength = getPasswordStrength(val);
        bar.style.width = strength.percent + '%';
        bar.style.backgroundColor = strength.color;
        setFieldState(this, hint, strength.label, strength.hintClass);

        // 비밀번호 확인 필드도 재검사
        const confirmInput = document.getElementById('password-confirm');
        if (confirmInput.value) {
            confirmInput.dispatchEvent(new Event('input'));
        }
    });

    // 비밀번호 확인: 일치 여부
    document.getElementById('password-confirm').addEventListener('input', function () {
        const pw = document.getElementById('password').value;
        const hint = document.getElementById('hint-confirm');
        if (this.value.length === 0) {
            setFieldState(this, hint, '', '');
        } else if (this.value === pw) {
            setFieldState(this, hint, '✓ 비밀번호가 일치합니다.', 'valid');
        } else {
            setFieldState(this, hint, '비밀번호가 일치하지 않습니다.', 'invalid');
        }
    });

    // 엔터키 회원가입
    document.getElementById('password-confirm').addEventListener('keypress', function (e) {
        if (e.key === 'Enter') signUp();
    });
});

// ─── 필드 상태 설정 헬퍼 ───
function setFieldState(inputEl, hintEl, message, type) {
    inputEl.classList.remove('is-valid', 'is-invalid');
    hintEl.classList.remove('valid', 'invalid');
    hintEl.textContent = message;

    if (type === 'valid') {
        inputEl.classList.add('is-valid');
        hintEl.classList.add('valid');
    } else if (type === 'invalid') {
        inputEl.classList.add('is-invalid');
        hintEl.classList.add('invalid');
    }
}

// ─── 비밀번호 강도 측정 ───
function getPasswordStrength(pw) {
    let score = 0;
    if (pw.length >= 8) score++;
    if (pw.length >= 12) score++;
    if (/[A-Z]/.test(pw)) score++;
    if (/[0-9]/.test(pw)) score++;
    if (/[^A-Za-z0-9]/.test(pw)) score++;

    if (score <= 1) return { percent: 20, color: '#ef4444', label: '매우 약함', hintClass: 'invalid' };
    if (score === 2) return { percent: 40, color: '#f97316', label: '약함', hintClass: 'invalid' };
    if (score === 3) return { percent: 65, color: '#facc15', label: '보통', hintClass: '' };
    if (score === 4) return { percent: 85, color: '#3b82f6', label: '강함', hintClass: 'valid' };
    return { percent: 100, color: '#10b981', label: '매우 강함 💪', hintClass: 'valid' };
}

// ─── 비밀번호 토글 ───
function togglePassword(inputId, btn) {
    const input = document.getElementById(inputId);
    const isText = input.type === 'text';
    input.type = isText ? 'password' : 'text';

    // 아이콘 교체 (눈 뜨기 / 눈 감기)
    btn.innerHTML = isText
        ? `<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
             <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
             <path stroke-linecap="round" stroke-linejoin="round" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>
           </svg>`
        : `<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
             <path stroke-linecap="round" stroke-linejoin="round" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88L6.59 6.59m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21"/>
           </svg>`;
}

// ─── 토스트 알림 ───
function showToast(message, isError = false) {
    const toast = document.getElementById('toast');
    toast.innerText = message;
    isError ? toast.classList.add('error') : toast.classList.remove('error');
    toast.classList.add('show');
    setTimeout(() => toast.classList.remove('show'), 3000);
}

// ─── 회원가입 제출 ───
async function signUp() {
    const name = document.getElementById('name').value.trim();
    const email = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value.trim();
    const passwordConfirm = document.getElementById('password-confirm').value.trim();

    // 클라이언트 측 최종 검증
    if (!name || !email || !password || !passwordConfirm) {
        showToast('모든 항목을 입력해주세요!', true);
        return;
    }
    if (name.length < 2) {
        showToast('이름은 2자 이상이어야 합니다.', true);
        return;
    }
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
        showToast('올바른 이메일 형식을 입력해주세요.', true);
        return;
    }
    if (password.length < 8) {
        showToast('비밀번호는 8자 이상이어야 합니다.', true);
        return;
    }
    if (password !== passwordConfirm) {
        showToast('비밀번호가 일치하지 않습니다.', true);
        return;
    }

    try {
        const response = await fetch('/api/v1/auth/signup', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            // SignUpRequestDto 필드명과 일치시켜주세요
            body: JSON.stringify({ name, email, password })
        });

        if (response.ok) {
            // 모든 스텝 완료 처리
            document.getElementById('step-dot-2').classList.remove('active');
            document.getElementById('step-dot-2').classList.add('done');
            document.getElementById('step-line-2').classList.add('done');
            document.getElementById('step-dot-3').classList.add('done');

            showToast('회원가입 성공! 잠시 후 이동합니다.');
            setTimeout(() => {
                window.location.href = '/api/v1/auth/loginP';
            }, 1800);
        } else {
            const errorText = await response.text();
            showToast(errorText || '회원가입에 실패했습니다.', true);
        }
    } catch (error) {
        console.error('통신 에러:', error);
        showToast('서버와 통신 중 문제가 발생했습니다.', true);
    }
}

async function sendEmailCode() {
    const email = document.getElementById('email').value.trim();
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

    if (!email || !emailRegex.test(email)) {
        showToast('올바른 이메일을 입력해주세요.', true);
        return;
    }

    try {
        // 백엔드 AuthService.sendVerificationCode 호출용 엔드포인트 가정
        const response = await fetch(`/api/v1/auth/email-verification?email=${email}`, {
            method: 'POST'
        });

        if (response.ok) {
            showToast('인증번호가 발송되었습니다. 메일함을 확인하세요!');
        } else {
            const msg = await response.text();
            showToast(msg || '발송 실패', true);
        }
    } catch (e) {
        showToast('서버 통신 에러', true);
    }
}

// ─── 이메일 인증번호 확인 ───
async function confirmEmailCode() {
    const email = document.getElementById('email').value.trim();
    const code = document.getElementById('verification-code').value.trim();
    const hint = document.getElementById('hint-verify');

    try {
        // 백엔드 AuthService.verifyCode 호출용 엔드포인트 가정
        const response = await fetch(`/api/v1/auth/email-verify?email=${email}&code=${code}`, {
            method: 'POST'
        });

        if (response.ok) {
            showToast('인증이 완료되었습니다!');

            // 스텝 업데이트: 1단계 완료, 2단계 활성화
            document.getElementById('step-dot-1').classList.add('done');
            document.getElementById('step-line-1').classList.add('done');
            document.getElementById('step-dot-2').classList.add('active');

            // 입력창 고정
            document.getElementById('verification-code').readOnly = true;
            document.getElementById('email').readOnly = true;
        } else {
            setFieldState(document.getElementById('verification-code'), hint, '인증번호가 틀렸습니다.', 'invalid');
        }
    } catch (e) {
        showToast('서버 통신 에러', true);
    }
}