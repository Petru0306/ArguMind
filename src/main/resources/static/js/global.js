(function () {
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;

    const dot = document.getElementById('cursor-dot');
    const ring = document.getElementById('cursor-ring');
    if (!dot || !ring) return;

    document.body.classList.add('has-custom-cursor');

    let mx = 0, my = 0, rx = 0, ry = 0;
    document.addEventListener('mousemove', (e) => {
        mx = e.clientX;
        my = e.clientY;
        dot.style.left = mx + 'px';
        dot.style.top = my + 'px';
    });

    function animateRing() {
        rx += (mx - rx) * 0.18;
        ry += (my - ry) * 0.18;
        ring.style.left = rx + 'px';
        ring.style.top = ry + 'px';
        requestAnimationFrame(animateRing);
    }
    animateRing();

    function bindHoverTargets() {
        document.querySelectorAll('a, button, .hover-card, .topic-card-interactive, .nav-interactive, tr[data-href]').forEach((el) => {
            if (el.dataset.cursorBound) return;
            el.dataset.cursorBound = '1';
            el.addEventListener('mouseenter', () => {
                ring.style.width = '28px';
                ring.style.height = '28px';
                ring.style.opacity = '0.32';
                ring.style.borderWidth = '1.5px';
            });
            el.addEventListener('mouseleave', () => {
                ring.style.width = '22px';
                ring.style.height = '22px';
                ring.style.opacity = '0.2';
                ring.style.borderWidth = '1px';
            });
        });
    }
    bindHoverTargets();
    new MutationObserver(bindHoverTargets).observe(document.body, { childList: true, subtree: true });

    document.querySelectorAll('tr[data-href]').forEach((row) => {
        row.style.cursor = 'none';
        row.addEventListener('click', () => {
            window.location.href = row.dataset.href;
        });
    });
})();
