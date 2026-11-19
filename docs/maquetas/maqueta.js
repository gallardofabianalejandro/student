/* Solo para las maquetas: tema claro/oscuro, paleta (a, b, c) y estado de la pantalla (#normal, #error, #loading, #empty). */
(function () {
  var root = document.documentElement;
  var PALETTES = [["b", "Verde y turquesa (elegida)"], ["a", "Verde fresco"], ["c", "Salvia y crema"]];

  function read(k) { try { return localStorage.getItem(k); } catch (e) { return null; } }
  function write(k, v) { try { localStorage.setItem(k, v); } catch (e) { /* sin almacenamiento */ } }
  function system() { return window.matchMedia && matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light"; }
  function validPalette(p) { return PALETTES.some(function (x) { return x[0] === p; }); }

  var theme = read("maqueta-theme");
  if (theme === "light" || theme === "dark") root.setAttribute("data-theme", theme);
  else if (!root.getAttribute("data-theme")) root.setAttribute("data-theme", system());

  var palette = read("maqueta-palette");
  // Paleta elegida: B (verde y turquesa). A y C quedan solo para comparar.
  root.setAttribute("data-palette", validPalette(palette) ? palette : "b");

  function syncSelects() {
    var cur = root.getAttribute("data-palette");
    document.querySelectorAll(".palette-select").forEach(function (s) { s.value = cur; });
  }

  function injectSelects() {
    document.querySelectorAll("[data-theme-toggle]").forEach(function (btn) {
      var prev = btn.previousElementSibling;
      if (prev && prev.classList.contains("palette-select")) return;
      var sel = document.createElement("select");
      sel.className = "palette-select";
      sel.setAttribute("aria-label", "Paleta de colores");
      PALETTES.forEach(function (p) {
        var o = document.createElement("option");
        o.value = p[0];
        o.textContent = p[1];
        sel.appendChild(o);
      });
      btn.parentNode.insertBefore(sel, btn);
    });
    syncSelects();
  }

  function applyState() {
    var s = location.hash.slice(1) || "normal";
    root.setAttribute("data-state", s);
    document.querySelectorAll(".chip").forEach(function (c) {
      if (c.getAttribute("href") === "#" + s) c.setAttribute("aria-current", "true");
      else c.removeAttribute("aria-current");
    });
  }

  document.addEventListener("click", function (e) {
    var btn = e.target.closest && e.target.closest("[data-theme-toggle]");
    if (!btn) return;
    var next = root.getAttribute("data-theme") === "dark" ? "light" : "dark";
    root.setAttribute("data-theme", next);
    write("maqueta-theme", next);
  });
  document.addEventListener("change", function (e) {
    var sel = e.target;
    if (!sel.classList || !sel.classList.contains("palette-select")) return;
    root.setAttribute("data-palette", sel.value);
    write("maqueta-palette", sel.value);
    syncSelects();
  });
  window.addEventListener("hashchange", applyState);
  document.addEventListener("DOMContentLoaded", function () { injectSelects(); applyState(); });
  if (document.readyState !== "loading") { injectSelects(); applyState(); }
})();
