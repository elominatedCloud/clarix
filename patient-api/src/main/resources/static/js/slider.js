// Horizontal carousel for the today screen (and similar).
// - Click prev / next arrows
// - Click dots
// - Pointer drag (swipe)
// Skips drag if the gesture starts on an interactive element (.envelope, button, a, input, textarea)
(function () {
  function init(slider) {
    const viewport = slider.querySelector('.slider-viewport');
    const track    = slider.querySelector('.slider-track');
    const slides   = slider.querySelectorAll('.slider-slide');
    const dots     = slider.querySelectorAll('.slider-dots .dot');
    const prev     = slider.querySelector('.slider-arrow.prev');
    const next     = slider.querySelector('.slider-arrow.next');
    if (!track || slides.length === 0) return;

    let index = parseInt(slider.dataset.index || '0', 10);
    if (index < 0) index = 0;
    if (index > slides.length - 1) index = slides.length - 1;
    const max = slides.length - 1;

    function syncHeight() {
      track.style.height = slides[index].scrollHeight + 'px';
    }

    function go(i, animate) {
      index = Math.max(0, Math.min(max, i));
      if (!animate) track.style.transition = 'none';
      track.style.transform = `translateX(-${index * 100}%)`;
      dots.forEach((d, j) => d.setAttribute('aria-current', j === index ? 'true' : 'false'));
      if (prev) prev.disabled = index === 0;
      if (next) next.disabled = index === max;
      syncHeight();
      if (!animate) {
        // re-enable transition on next frame
        requestAnimationFrame(() => { track.style.transition = ''; });
      }
    }

    if (prev) prev.addEventListener('click', () => go(index - 1, true));
    if (next) next.addEventListener('click', () => go(index + 1, true));
    dots.forEach((d, j) => d.addEventListener('click', () => go(j, true)));

    // swipe
    let startX = 0, dx = 0, dragging = false, pid = null;
    function onDown(e) {
      if (e.target.closest('.envelope, button, a, input, textarea, label')) return;
      dragging = true;
      pid = e.pointerId;
      startX = e.clientX;
      dx = 0;
      track.style.transition = 'none';
    }
    function onMove(e) {
      if (!dragging) return;
      dx = e.clientX - startX;
      // resistance at edges
      if ((index === 0 && dx > 0) || (index === max && dx < 0)) dx *= 0.35;
      track.style.transform = `translateX(calc(-${index * 100}% + ${dx}px))`;
    }
    function onUp() {
      if (!dragging) return;
      dragging = false;
      track.style.transition = '';
      const w = viewport.clientWidth;
      const threshold = Math.min(60, w * 0.18);
      if (dx <= -threshold) go(index + 1, true);
      else if (dx >= threshold) go(index - 1, true);
      else go(index, true);
    }
    slider.addEventListener('pointerdown', onDown);
    slider.addEventListener('pointermove', onMove);
    slider.addEventListener('pointerup', onUp);
    slider.addEventListener('pointercancel', onUp);

    // keep height accurate when window resizes (rotation, dev tools, etc.)
    window.addEventListener('resize', () => requestAnimationFrame(syncHeight));

    // initial position without animation
    go(index, false);
  }

  function start() { document.querySelectorAll('.clarix-slider').forEach(init); }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', start);
  else start();
})();
