(function() {
  'use strict';

  const GITHUB_REPO = 'zer0k7/Chronicle';
  const API_URL = `https://api.github.com/repos/${GITHUB_REPO}/releases/latest`;

  let cachedRelease = null;
  let cachedApkAsset = null;

  function showToast(msg) {
    const toast = document.getElementById('toast');
    if (!toast) return;
    const span = toast.querySelector('span');
    if (span) span.textContent = msg;
    toast.classList.add('show');
    setTimeout(() => {
      toast.classList.remove('show');
    }, 3500);
  }

  async function fetchLatestRelease() {
    try {
      const res = await fetch(API_URL, {
        headers: { 'Accept': 'application/vnd.github+json' }
      });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const data = await res.json();
      cachedRelease = data;
      cachedApkAsset = data.assets && data.assets.find(a => a.name && a.name.endsWith('.apk'));
      return data;
    } catch (err) {
      console.warn('GitHub API fetch failed, using fallback data:', err);
      return null;
    }
  }

  function formatMarkdown(text) {
    if (!text) return '<p>No release notes provided.</p>';
    
    let html = text
      .replace(/^### (.*$)/gim, '<h4>$1</h4>')
      .replace(/^## (.*$)/gim, '<h3>$1</h3>')
      .replace(/^# (.*$)/gim, '<h2>$1</h2>')
      .replace(/\*\*(.*?)\*\*/gim, '<strong>$1</strong>')
      .replace(/^\* (.*$)/gim, '<li>$1</li>')
      .replace(/^- (.*$)/gim, '<li>$1</li>')
      .replace(/^---$/gim, '<hr style="border:none;border-top:1px solid var(--border);margin:12px 0;">')
      .replace(/\n\n+/g, '<br/>');

    html = html.replace(/(<li>[\s\S]*?<\/li>)/g, '<ul>$1</ul>');
    html = html.replace(/<\/ul>\s*<ul>/g, '');

    return html;
  }

  async function initPageData() {
    const fallbackVersion = document.body.dataset.version || 'v1.0.9';
    const fallbackDateStr = document.body.dataset.releaseDate || '2026-08-25';

    const heroVersionEl = document.getElementById('hero-version');
    const downloadVersionEl = document.getElementById('download-version');
    const releaseDateEl = document.getElementById('release-date');
    const fileSizeEl = document.getElementById('file-size');
    const releaseNotesEl = document.getElementById('release-notes-body');

    const release = await fetchLatestRelease();

    if (release) {
      const versionTag = release.tag_name || fallbackVersion;
      const pubDate = release.published_at
        ? new Date(release.published_at).toLocaleDateString('en-IN', { day: 'numeric', month: 'long', year: 'numeric' })
        : fallbackDateStr;

      if (heroVersionEl) heroVersionEl.textContent = versionTag;
      if (downloadVersionEl) downloadVersionEl.textContent = versionTag;
      if (releaseDateEl) releaseDateEl.textContent = pubDate;

      if (cachedApkAsset && cachedApkAsset.size) {
        const sizeMb = (cachedApkAsset.size / (1024 * 1024)).toFixed(1);
        if (fileSizeEl) fileSizeEl.textContent = `${sizeMb} MB`;
      } else {
        if (fileSizeEl) fileSizeEl.textContent = '~2.0 MB';
      }

      if (releaseNotesEl && release.body) {
        releaseNotesEl.innerHTML = formatMarkdown(release.body);
      }
    } else {
      if (heroVersionEl) heroVersionEl.textContent = fallbackVersion;
      if (downloadVersionEl) downloadVersionEl.textContent = fallbackVersion;
      if (releaseDateEl) releaseDateEl.textContent = fallbackDateStr;
      if (fileSizeEl) fileSizeEl.textContent = '~2.0 MB';
      if (releaseNotesEl) {
        releaseNotesEl.innerHTML = '<p>Conscious waking life calculations, ghost reflex loop detector, morning bed screen tracker, and hardened auto-updater.</p>';
      }
    }
  }

  window.triggerDownload = async function(customUrl, customFilename, element) {
    const btn = document.getElementById('download-btn');
    const originalText = btn ? btn.innerHTML : 'Download APK';

    if (btn) {
      btn.textContent = 'Preparing Download...';
      btn.disabled = true;
    }

    try {
      if (!cachedRelease && !customUrl) {
        await fetchLatestRelease();
      }

      let downloadUrl = customUrl;
      let filename = customFilename || 'chronicle-latest.apk';

      if (!downloadUrl) {
        if (cachedApkAsset && cachedApkAsset.browser_download_url) {
          downloadUrl = cachedApkAsset.browser_download_url;
          filename = cachedApkAsset.name;
        } else {
          const fallbackVersion = document.body.dataset.version || 'v1.0.9';
          downloadUrl = `https://github.com/${GITHUB_REPO}/releases/download/${fallbackVersion}/chronicle-${fallbackVersion}.apk`;
          filename = `chronicle-${fallbackVersion}.apk`;
        }
      }

      showToast('Downloading ' + filename + '...');

      // Silent hidden iframe download trigger (prevents any redirection or tab opening)
      const iframe = document.createElement('iframe');
      iframe.style.display = 'none';
      iframe.src = downloadUrl;
      document.body.appendChild(iframe);

      setTimeout(() => {
        if (iframe && iframe.parentNode) {
          iframe.parentNode.removeChild(iframe);
        }
      }, 60000);

      if (btn) {
        btn.textContent = 'Download Started';
        setTimeout(() => {
          btn.innerHTML = originalText;
          btn.disabled = false;
        }, 3000);
      }
    } catch (err) {
      console.error('Download error:', err);
      showToast('Download error. Retrying direct link...');
      if (btn) {
        btn.textContent = 'Download APK';
        btn.disabled = false;
      }
    }
  };

  // Accordion Toggle
  window.toggleAccordion = function(id) {
    const content = document.getElementById(id);
    const trigger = document.querySelector(`[aria-controls="${id}"]`);
    if (!content || !trigger) return;

    const isOpen = content.classList.contains('open');
    if (isOpen) {
      content.classList.remove('open');
      trigger.setAttribute('aria-expanded', 'false');
    } else {
      content.classList.add('open');
      trigger.setAttribute('aria-expanded', 'true');
    }
  };

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initPageData);
  } else {
    initPageData();
  }
})();
