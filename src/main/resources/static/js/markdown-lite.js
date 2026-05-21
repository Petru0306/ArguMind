/**
 * Lightweight Markdown → HTML (bold, italic, lists, hr, paragraphs). No external deps.
 */
function escapeHtmlLite(text) {
    const d = document.createElement('div');
    d.textContent = text;
    return d.innerHTML;
}

function renderMarkdownLite(text) {
    if (!text) return '';

    const lines = text.replace(/\r\n/g, '\n').split('\n');
    const out = [];
    let inList = false;

    function closeList() {
        if (inList) {
            out.push('</ul>');
            inList = false;
        }
    }

    function inlineFormat(s) {
        let h = escapeHtmlLite(s);
        h = h.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
        h = h.replace(/__(.+?)__/g, '<strong>$1</strong>');
        h = h.replace(/(?<!\*)\*([^*]+)\*(?!\*)/g, '<em>$1</em>');
        return h;
    }

    for (const raw of lines) {
        const line = raw.trim();
        if (line === '---' || line === '***') {
            closeList();
            out.push('<hr class="my-3 border-chai-dark/15">');
            continue;
        }
        if (/^[-*]\s+/.test(line)) {
            if (!inList) {
                out.push('<ul class="list-disc pl-5 my-2 space-y-1">');
                inList = true;
            }
            out.push('<li>' + inlineFormat(line.replace(/^[-*]\s+/, '')) + '</li>');
            continue;
        }
        closeList();
        if (line === '') {
            out.push('<br>');
            continue;
        }
        if (line.startsWith('### ')) {
            out.push('<h3 class="font-bold text-chai-dark mt-3 mb-1">' + inlineFormat(line.slice(4)) + '</h3>');
        } else if (line.startsWith('## ')) {
            out.push('<h2 class="font-bold text-chai-dark mt-3 mb-1">' + inlineFormat(line.slice(3)) + '</h2>');
        } else {
            out.push('<p class="my-1">' + inlineFormat(line) + '</p>');
        }
    }
    closeList();
    return out.join('');
}

function renderCoachMarkdown(text) {
    return renderMarkdownLite(text);
}
