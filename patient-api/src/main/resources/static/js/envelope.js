// Envelope tear interaction — drag horizontally to "tear" the medication packet.
// Each .envelope has [data-form] = id of a form to submit when torn.
(function () {
  const THRESHOLD = 70; // px the user must drag horizontally to commit a tear
  const MAX = 110;

  function bind(env) {
    if (env.dataset.bound === '1') return;
    env.dataset.bound = '1';

    if (env.classList.contains('is-torn')
        || env.classList.contains('is-locked-future')
        || env.classList.contains('is-locked-past')) return;

    const top = env.querySelector('.env-half.top');
    const bot = env.querySelector('.env-half.bottom');
    if (!top || !bot) return;

    let startX = 0, startY = 0, dx = 0, dragging = false, pid = null;

    function setOffset(d) {
      // 큐가 왼쪽에 있으므로 왼쪽 드래그는 시각·로직 모두 무시. 오른쪽으로 밀어 뜯기만 허용.
      const clamped = Math.max(0, Math.min(MAX, d));
      const yMag = Math.min(clamped * 0.32, 36);
      const rot  = clamped * 0.06;
      top.style.transform = `translate(${clamped * 0.6}px, ${-yMag}px) rotate(${-rot}deg)`;
      bot.style.transform = `translate(${-clamped * 0.6}px, ${yMag}px) rotate(${rot}deg)`;
    }

    function onDown(e) {
      if (env.classList.contains('is-torn')) return;
      dragging = true;
      pid = e.pointerId;
      startX = e.clientX;
      startY = e.clientY;
      dx = 0;
      env.classList.add('is-dragging');
      try { env.setPointerCapture(pid); } catch (_) {}
    }

    function onMove(e) {
      if (!dragging) return;
      const ddx = e.clientX - startX;
      const ddy = e.clientY - startY;
      // ignore if it's clearly a vertical scroll
      if (Math.abs(ddy) > Math.abs(ddx) + 12 && Math.abs(ddx) < 8) return;
      e.preventDefault();
      dx = ddx;
      setOffset(dx);
    }

    function commit() {
      // 테스트 모드: 4h 컨펌 우회. 운영에선 data-last-taken 검사 재도입.
      env.classList.remove('is-dragging');
      env.classList.add('is-tearing');
      env.classList.add('is-torn');
      top.style.transform = '';
      bot.style.transform = '';

      // Haptic — Android & some Chromium builds; ignored elsewhere
      if (typeof navigator !== 'undefined' && navigator.vibrate) {
        try { navigator.vibrate(15); } catch (_) {}
      }

      setTimeout(() => env.classList.remove('is-tearing'), 220);

      const formId = env.dataset.form;
      if (formId) {
        const f = document.getElementById(formId);
        if (f) {
          // wait for tear animation + check-pop to play before navigating
          setTimeout(() => f.submit(), 720);
        }
      }
    }

    function reset() {
      env.classList.remove('is-dragging');
      top.style.transform = '';
      bot.style.transform = '';
    }

    function onUp() {
      if (!dragging) return;
      dragging = false;
      try { env.releasePointerCapture(pid); } catch (_) {}
      // 오른쪽으로 임계치 이상 끌었을 때만 tear (큐는 왼쪽에 있음)
      if (dx >= THRESHOLD) commit();
      else reset();
    }

    env.addEventListener('pointerdown', onDown);
    env.addEventListener('pointermove', onMove);
    env.addEventListener('pointerup', onUp);
    env.addEventListener('pointercancel', onUp);
  }

  function init() { document.querySelectorAll('.envelope').forEach(bind); }
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
