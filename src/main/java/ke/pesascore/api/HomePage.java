package ke.pesascore.api;

/** A single self-contained page for trying the API without curl. */
final class HomePage {

    private HomePage() {
    }

    static final String HTML = """
            <!doctype html>
            <html lang="en"><head><meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <title>PesaScore</title>
            <style>
              body{font:16px/1.5 system-ui,sans-serif;max-width:760px;margin:40px auto;padding:0 16px;color:#1b1f1d;background:#f6f4ee}
              h1{margin:0 0 4px} p{color:#555}
              .box{background:#fff;border:1px solid #e3dfd3;border-radius:12px;padding:20px;margin-top:20px}
              button{background:#1f7a4d;color:#fff;border:0;border-radius:8px;padding:10px 18px;font-size:15px;cursor:pointer}
              .score{font-size:56px;font-weight:700;color:#1f7a4d;line-height:1}
              li{margin:4px 0} .muted{color:#777;font-size:14px} .err{color:#b3261e}
            </style></head><body>
            <h1>PesaScore</h1>
            <p>Upload an M-Pesa statement (CSV) to get a credit score, a safe loan limit and the reasons behind them.
            The file is scored in memory and never stored.</p>
            <div class="box">
              <input type="file" id="file" accept=".csv,text/csv"> <button id="go">Score statement</button>
            </div>
            <div class="box" id="out" hidden></div>
            <script>
            const esc = s => String(s).replace(/[&<>"]/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[c]));
            const kes = n => 'KES ' + Number(n).toLocaleString('en-KE');
            document.getElementById('go').onclick = async () => {
              const f = document.getElementById('file').files[0];
              const out = document.getElementById('out');
              if (!f) return;
              out.hidden = false; out.textContent = 'Scoring...';
              const res = await fetch('/api/v1/score', {method: 'POST', headers: {'Content-Type': 'text/csv'}, body: await f.text()});
              const r = await res.json();
              if (!res.ok) { out.innerHTML = '<p class="err">' + esc(r.error) + '</p>'; return; }
              const list = xs => xs.map(x => '<li>' + esc(x.message) + '</li>').join('');
              out.innerHTML = '<div class="score">' + r.score + '</div><p>Band ' + r.band + ' &middot; ' + esc(r.bandLabel)
                + ' &middot; confidence ' + r.confidence.toLowerCase() + '</p>'
                + (r.offer.approved
                   ? '<p><b>Suggested limit ' + kes(r.offer.limitKes) + '</b> over ' + r.offer.termMonths + ' month(s), '
                     + kes(r.offer.monthlyInstalmentKes) + ' a month</p>'
                   : '<p><b>No loan suggested</b></p>')
                + '<p class="muted">' + esc(r.offer.rationale) + '</p>'
                + (r.concerns.length ? '<h3>Holding the score back</h3><ul>' + list(r.concerns) + '</ul>' : '')
                + (r.strengths.length ? '<h3>Helping</h3><ul>' + list(r.strengths) + '</ul>' : '');
            };
            </script></body></html>
            """;
}
