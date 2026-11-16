// Fills the download page from latest.json. No libraries, and nothing is loaded from other sites.
(function () {
  var status = document.getElementById('status');

  function size(bytes) {
    if (!bytes) return 'about 16 MB';
    return (bytes / 1048576).toFixed(1) + ' MB';
  }

  function androidName(api) {
    var names = { 26: '8.0', 27: '8.1', 28: '9', 29: '10', 30: '11', 31: '12', 32: '12L', 33: '13', 34: '14', 35: '15', 36: '16' };
    return names[api] || ('(API ' + api + ')');
  }

  fetch('/latest.json', { cache: 'no-store' })
    .then(function (r) { if (!r.ok) throw new Error('none'); return r.json(); })
    .then(function (m) {
      if (!m || !m.versionName || !/^[0-9a-f]{64}$/i.test(m.sha256 || '')) throw new Error('bad');
      document.getElementById('v-name').textContent = m.versionName;
      document.getElementById('v-size').textContent = size(m.sizeBytes);
      document.getElementById('v-min').textContent = 'Android ' + androidName(m.minAndroid || 26) + ' or newer';
      document.getElementById('v-sha').textContent = m.sha256.toLowerCase();
      if (m.notes) {
        document.getElementById('v-notes').textContent = m.notes;
        document.getElementById('notes-box').hidden = false;
      }
      document.getElementById('facts').hidden = false;
      document.getElementById('actions').hidden = false;
      status.textContent = 'The latest version is ready.';
    })
    .catch(function () {
      status.textContent = 'The first release is not published yet. Please check back soon.';
    });
})();
