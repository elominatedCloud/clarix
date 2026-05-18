(function () {
  const storageKey = 'clarix-doctor-theme';

  function currentTheme() {
    return document.documentElement.dataset.theme === 'dark' ? 'dark' : 'light';
  }

  function applyTheme(theme) {
    const next = theme === 'dark' ? 'dark' : 'light';
    document.documentElement.dataset.theme = next;
    document.querySelectorAll('[data-theme-toggle]').forEach((toggle) => {
      toggle.checked = next === 'dark';
    });
    document.querySelectorAll('[data-theme-label]').forEach((label) => {
      label.textContent = next === 'dark' ? 'Dark' : 'Light';
    });
    try {
      localStorage.setItem(storageKey, next);
    } catch (e) {
      // Ignore storage failures; the page-level theme still updates.
    }
  }

  document.addEventListener('DOMContentLoaded', function () {
    applyTheme(currentTheme());
    document.querySelectorAll('[data-theme-toggle]').forEach((toggle) => {
      toggle.addEventListener('change', function () {
        applyTheme(toggle.checked ? 'dark' : 'light');
        window.dispatchEvent(new CustomEvent('clarix-theme-change', {
          detail: { theme: currentTheme() },
        }));
      });
    });
  });
})();
