package com.tkno.ren.util

import org.json.JSONObject

object ReaderModeManager {

    const val EXIT_READER_URL = "ren://exit_reader_mode"

    fun getExtractionScript(): String {
        return """
        (function() {
            try {
                var ogTitle = document.querySelector('meta[property="og:title"]');
                var title = (ogTitle && ogTitle.content) || (document.querySelector('h1') && document.querySelector('h1').innerText) || document.title || '';
                title = title.trim();

                var ogSite = document.querySelector('meta[property="og:site_name"]');
                var siteName = (ogSite && ogSite.content) || window.location.hostname.replace(/^www\./, '');

                var metaAuthor = document.querySelector('meta[name="author"], meta[property="article:author"], [rel="author"], .author, .byline, .author-name');
                var author = (metaAuthor && (metaAuthor.content || metaAuthor.innerText)) || '';
                author = author.trim();

                var removeSelectors = 'script, style, noscript, iframe, nav, aside, footer, header, form, .ad, .ads, .advertisement, .social-share, .comments, .related-posts, .sidebar, .popup, [role="banner"], [role="navigation"], [role="complementary"]';
                
                var candidates = document.querySelectorAll('article, main, [role="main"], .post-content, .entry-content, .article-body, .article-content, .story-body, .content, #content, #main-content');
                var bestElement = null;
                var maxScore = 0;

                if (candidates.length > 0) {
                    for (var i = 0; i < candidates.length; i++) {
                        var el = candidates[i];
                        var pCount = el.querySelectorAll('p').length;
                        var textLength = el.innerText ? el.innerText.length : 0;
                        var score = pCount * 50 + textLength;
                        if (score > maxScore) {
                            maxScore = score;
                            bestElement = el;
                        }
                    }
                }

                if (!bestElement || maxScore < 200) {
                    var allDivs = document.querySelectorAll('div, section');
                    for (var j = 0; j < allDivs.length; j++) {
                        var d = allDivs[j];
                        var pList = d.querySelectorAll('p');
                        if (pList.length >= 2) {
                            var textLen = d.innerText ? d.innerText.length : 0;
                            var sc = pList.length * 50 + textLen;
                            if (sc > maxScore) {
                                maxScore = sc;
                                bestElement = d;
                            }
                        }
                    }
                }

                if (!bestElement) {
                    bestElement = document.body;
                }

                if (!bestElement || (bestElement.innerText && bestElement.innerText.trim().length < 50)) {
                    return JSON.stringify({ success: false });
                }

                var clone = bestElement.cloneNode(true);
                var toRemove = clone.querySelectorAll(removeSelectors);
                for (var k = 0; k < toRemove.length; k++) {
                    if (toRemove[k].parentNode) {
                        toRemove[k].parentNode.removeChild(toRemove[k]);
                    }
                }

                var allElems = clone.querySelectorAll('*');
                for (var m = 0; m < allElems.length; m++) {
                    var elem = allElems[m];
                    elem.removeAttribute('style');
                    elem.removeAttribute('onclick');
                    elem.removeAttribute('onload');
                    elem.removeAttribute('onerror');
                    elem.removeAttribute('class');
                    elem.removeAttribute('id');
                    if (elem.tagName === 'IMG') {
                        if (elem.dataset && elem.dataset.src) {
                            elem.src = elem.dataset.src;
                        }
                        if (elem.src && !elem.src.startsWith('http') && !elem.src.startsWith('data:')) {
                            try {
                                elem.src = new URL(elem.getAttribute('src'), window.location.href).href;
                            } catch(e){}
                        }
                    }
                }

                return JSON.stringify({
                    success: true,
                    title: title,
                    author: author,
                    siteName: siteName,
                    content: clone.innerHTML
                });
            } catch(err) {
                return JSON.stringify({ success: false, error: err.toString() });
            }
        })();
        """.trimIndent()
    }

    fun parseExtractionResult(jsonStr: String): ArticleData? {
        if (jsonStr.isBlank() || jsonStr == "null") return null
        return try {
            // evaluateJavascript returns JSON string, which might be double escaped if returning a string
            val raw = if (jsonStr.startsWith("\"") && jsonStr.endsWith("\"")) {
                org.json.JSONTokener(jsonStr).nextValue().toString()
            } else {
                jsonStr
            }
            val obj = JSONObject(raw)
            if (obj.optBoolean("success", false)) {
                ArticleData(
                    title = obj.optString("title", "Article"),
                    author = obj.optString("author", ""),
                    siteName = obj.optString("siteName", ""),
                    contentHtml = obj.optString("content", "")
                )
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun generateReaderHtml(
        article: ArticleData,
        isDark: Boolean
    ): String {
        val themeClass = if (isDark) "dark" else "light"
        val title = article.title.ifBlank { "Article" }
        val siteName = article.siteName
        val author = article.author

        val metaParts = mutableListOf<String>()
        if (siteName.isNotBlank()) metaParts.add("<span>$siteName</span>")
        if (author.isNotBlank()) metaParts.add("<span>By $author</span>")
        val metaHtml = metaParts.joinToString(" • ")

        return """
        <!DOCTYPE html>
        <html dir="auto">
        <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=3.0, user-scalable=yes">
        <title>$title</title>
        <style>
          :root {
            --bg-color: #FAFAFC;
            --text-color: #1F2328;
            --meta-color: #656D76;
            --link-color: #1A73E8;
            --card-bg: #F0F2F5;
            --border-color: #E1E4E8;
            --btn-bg: #EAECEF;
            --btn-text: #24292F;
          }
          body.dark {
            --bg-color: #121517;
            --text-color: #E2E4E8;
            --meta-color: #8B949E;
            --link-color: #58A6FF;
            --card-bg: #1B1F23;
            --border-color: #30363D;
            --btn-bg: #21262D;
            --btn-text: #C9D1D9;
          }
          body.light {
            --bg-color: #FAFAFC;
            --text-color: #1F2328;
            --meta-color: #656D76;
            --link-color: #1A73E8;
            --card-bg: #F0F2F5;
            --border-color: #E1E4E8;
            --btn-bg: #EAECEF;
            --btn-text: #24292F;
          }
          body {
            margin: 0;
            padding: 24px 18px 80px 18px;
            background-color: var(--bg-color);
            color: var(--text-color);
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
            font-size: 18px;
            line-height: 1.8;
            word-wrap: break-word;
            text-rendering: optimizeLegibility;
            -webkit-font-smoothing: antialiased;
          }
          .reader-container {
            max-width: 680px;
            margin: 0 auto;
          }
          .top-banner {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 24px;
            padding-bottom: 12px;
            border-bottom: 1px solid var(--border-color);
          }
          .exit-btn {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            background-color: var(--btn-bg);
            color: var(--btn-text);
            padding: 8px 16px;
            border-radius: 20px;
            text-decoration: none;
            font-size: 13.5px;
            font-weight: 600;
          }
          .badge {
            font-size: 12px;
            font-weight: 700;
            text-transform: uppercase;
            letter-spacing: 0.5px;
            color: var(--meta-color);
          }
          h1.reader-title {
            font-size: 28px;
            line-height: 1.35;
            margin: 0 0 12px 0;
            font-weight: 700;
            letter-spacing: -0.01em;
          }
          .reader-meta {
            font-size: 14px;
            color: var(--meta-color);
            margin-bottom: 28px;
          }
          .reader-content p {
            margin: 0 0 20px 0;
          }
          .reader-content h2, .reader-content h3, .reader-content h4 {
            margin: 32px 0 14px 0;
            line-height: 1.4;
          }
          .reader-content img {
            max-width: 100%;
            height: auto;
            border-radius: 12px;
            margin: 20px 0;
            display: block;
            box-shadow: 0 4px 12px rgba(0,0,0,0.08);
          }
          .reader-content blockquote {
            margin: 20px 0;
            padding: 12px 20px;
            border-left: 4px solid var(--link-color);
            background-color: var(--card-bg);
            border-radius: 0 8px 8px 0;
            font-style: italic;
          }
          .reader-content a {
            color: var(--link-color);
            text-decoration: underline;
            text-underline-offset: 3px;
          }
          .reader-content pre, .reader-content code {
            background-color: var(--card-bg);
            border-radius: 6px;
            font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
            font-size: 14.5px;
          }
          .reader-content pre {
            padding: 14px;
            overflow-x: auto;
          }
          .reader-content ul, .reader-content ol {
            margin: 0 0 20px 0;
            padding-left: 24px;
          }
          .reader-content li {
            margin-bottom: 8px;
          }
        </style>
        </head>
        <body class="$themeClass">
          <div class="reader-container">
            <div class="top-banner">
              <span class="badge">📖 Reader View</span>
              <a href="$EXIT_READER_URL" class="exit-btn">✕ Exit Reader Mode</a>
            </div>
            <h1 class="reader-title">$title</h1>
            <div class="reader-meta">
              $metaHtml
            </div>
            <div class="reader-content">
              ${article.contentHtml}
            </div>
          </div>
        </body>
        </html>
        """.trimIndent()
    }

    data class ArticleData(
        val title: String,
        val author: String,
        val siteName: String,
        val contentHtml: String
    )
}
