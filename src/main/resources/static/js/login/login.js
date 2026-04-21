// 💡 추가됨: 마우스 추적 및 입력 필드 상호작용 로직
document.addEventListener('DOMContentLoaded', () => {
    const pupils = document.querySelectorAll('.pupil');
    const eyes = document.querySelectorAll('.eye');
    const inputs = document.querySelectorAll('input');

    // 1. 마우스 움직임에 따라 눈동자 움직이기
    document.addEventListener('mousemove', (e) => {
        // 입력 필드에 포커스가 가 있으면 마우스 추적 중단
        const isInputFocused = Array.from(inputs).some(input => input === document.activeElement);
        if (isInputFocused) return;

        pupils.forEach(pupil => {
            const eye = pupil.parentElement;
            if (eye.classList.contains('eye-closed')) return; // 눈 감고 있으면 움직이지 않음

            // 눈의 중심 좌표 가져오기
            const eyeRect = eye.getBoundingClientRect();
            const eyeCenterX = eyeRect.left + eyeRect.width / 2;
            const eyeCenterY = eyeRect.top + eyeRect.height / 2;

            // 마우스 커서와 눈 중심 사이의 각도 계산
            const angle = Math.atan2(e.clientY - eyeCenterY, e.clientX - eyeCenterX);

            // 눈동자가 움직일 거리 제한 (눈 크기에 맞춰 조정)
            const distance = 5;

            // 새로운 눈동자 위치 계산
            const pupilX = Math.cos(angle) * distance;
            const pupilY = Math.sin(angle) * distance;

            // 눈동자 위치 업데이트 (transform: translate 사용)
            pupil.style.transform = `translate(${pupilX}px, ${pupilY}px)`;
        });
    });

    // 2. 입력 필드에 포커스가 갈 때 눈 감고, 빠질 때 뜨게 하기
    inputs.forEach(input => {
        // 포커스 시: 모든 눈 감기
        input.addEventListener('focus', () => {
            eyes.forEach(eye => {
                eye.classList.add('eye-closed');
            });
            // 눈동자 위치 초기화
            pupils.forEach(pupil => {
                pupil.style.transform = `translate(0px, 0px)`;
            });
        });

        // 포커스 해제 시: 모든 눈 뜨기
        input.addEventListener('blur', () => {
            eyes.forEach(eye => {
                eye.classList.remove('eye-closed');
            });
        });
    });

    // 엔터키 로그인 기능
    document.getElementById('password').addEventListener('keypress', function (e) {
        if (e.key === 'Enter') {
            login();
        }
    });
});

// 💡 토스트 알림을 띄워주는 공통 함수
function showToast(message, isError = false) {
    const toast = document.getElementById("toast");
    toast.innerText = message;

    // 에러면 빨간색, 성공이면 초록색
    if (isError) {
        toast.classList.add("error");
    } else {
        toast.classList.remove("error");
    }

    // 알림 보여주기
    toast.classList.add("show");

    // 3초 뒤에 알림 다시 숨기기
    setTimeout(function(){
        toast.classList.remove("show");
    }, 3000);
}

// 로그인 함수 (수정됨)
// 💡 표정을 변경하는 함수
function setExpression(type) {
    const eyes = document.querySelectorAll('.eye');

    // 기존 상태 클래스 모두 제거
    eyes.forEach(eye => {
        eye.classList.remove('eye-closed', 'eye-smile', 'eye-sad');

        // 결과에 따른 클래스 추가
        if (type === 'smile') eye.classList.add('eye-smile');
        if (type === 'sad') eye.classList.add('eye-sad');
    });
}

// 수정된 login 함수
async function login() {
    const emailInput = document.getElementById('email').value.trim();
    const passwordInput = document.getElementById('password').value.trim();

    if (!emailInput || !passwordInput) {
        showToast("이메일과 비밀번호를 모두 입력해주세요!", true);
        return;
    }

    try {
        const response = await fetch('/api/v1/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email: emailInput, password: passwordInput })
        });

        if (response.ok) {
            // ✅ 로그인 성공: ^^ 표정으로 변경
            setExpression('smile');
            showToast("로그인 성공! 면접장으로 이동합니다... 🚀");

            setTimeout(() => {
                window.location.href = '/api/v1/interview/feedback';
            }, 1500);

        } else {
            // ❌ 로그인 실패: ㅠㅠ 표정으로 변경
            setExpression('sad');
            showToast("❌ 아이디나 비밀번호가 틀렸습니다.", true);

            // 2초 뒤에 다시 원래대로(눈 뜨기) 돌려놓고 싶다면 추가
            setTimeout(() => {
                const eyes = document.querySelectorAll('.eye');
                eyes.forEach(eye => eye.classList.remove('eye-sad'));
            }, 2000);
        }
    } catch (error) {
        console.error("통신 에러:", error);
        setExpression('sad');
        showToast("서버와 통신 중 문제가 발생했습니다.", true);
    }
}